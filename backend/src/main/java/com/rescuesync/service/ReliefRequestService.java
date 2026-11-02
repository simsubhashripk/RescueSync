package com.rescuesync.service;

import com.rescuesync.dto.QueueSummaryDTO;
import com.rescuesync.dto.ReliefRequestCreateDTO;
import com.rescuesync.dto.ReliefRequestResponseDTO;
import com.rescuesync.entity.AffectedRegion;
import com.rescuesync.entity.ReliefRequest;
import com.rescuesync.exception.ConflictException;
import com.rescuesync.exception.InvalidRegionDataException;
import com.rescuesync.exception.ResourceNotFoundException;
import com.rescuesync.repository.AffectedRegionRepository;
import com.rescuesync.repository.AllocationRepository;
import com.rescuesync.repository.ReliefRequestRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class ReliefRequestService {
public static final String STATUS_PENDING = "PENDING";
public static final String STATUS_PARTIALLY_ALLOCATED = "PARTIALLY_ALLOCATED";
public static final String STATUS_ALLOCATED = "ALLOCATED";

private final ReliefRequestRepository reliefRequestRepository;
private final AffectedRegionRepository affectedRegionRepository;
private final AllocationRepository allocationRepository;

/**
 * Dual-criterion Comparator matching desktop ReliefRequest:
 * 1. Higher priority score first (descending).
 * 2. Earlier arrival order first (ascending FIFO tie-breaker).
 */
private static final Comparator<ReliefRequest> PRIORITY_COMPARATOR = (a, b) -> {
    if (a == null && b == null) return 0;
    if (a == null) return 1;
    if (b == null) return -1;

    int cmp = Double.compare(b.getPriorityScore(), a.getPriorityScore());

    if (cmp != 0) {
        return cmp;
    }

    return Integer.compare(
            a.getRequestArrivalOrder(),
            b.getRequestArrivalOrder()
    );
};

public ReliefRequestService(
        ReliefRequestRepository reliefRequestRepository,
        AffectedRegionRepository affectedRegionRepository,
        AllocationRepository allocationRepository) {

    this.reliefRequestRepository = reliefRequestRepository;
    this.affectedRegionRepository = affectedRegionRepository;
    this.allocationRepository = allocationRepository;
}

/**
 * Retrieve all relief requests ordered by priority score descending
 * and arrival order ascending.
 */
@Transactional(readOnly = true)
public List<ReliefRequestResponseDTO> getAllRequests() {

    List<ReliefRequest> allRequests =
            reliefRequestRepository
                    .findAllByOrderByPriorityScoreDescRequestArrivalOrderAsc();

    Map<Integer, Integer> queueRanks =
            computeActiveQueueRanks(allRequests);

    return allRequests.stream()
            .map(r -> ReliefRequestResponseDTO.fromEntity(
                    r,
                    queueRanks.get(r.getRequestId())
            ))
            .collect(Collectors.toList());
}

/**
 * Snapshot of the active triage queue using java.util.PriorityQueue.
 * Contains only PENDING and PARTIALLY_ALLOCATED demands.
 */
@Transactional(readOnly = true)
public List<ReliefRequestResponseDTO> getActivePriorityQueue() {

    List<ReliefRequest> activeList =
            reliefRequestRepository
                    .findByRequestStatusInOrderByPriorityScoreDescRequestArrivalOrderAsc(
                            List.of(
                                    STATUS_PENDING,
                                    STATUS_PARTIALLY_ALLOCATED
                            )
                    );

    PriorityQueue<ReliefRequest> queue =
            new PriorityQueue<>(PRIORITY_COMPARATOR);

    queue.addAll(activeList);

    List<ReliefRequestResponseDTO> orderedList =
            new ArrayList<>();

    int rank = 1;

    while (!queue.isEmpty()) {

        ReliefRequest polled = queue.poll();

        orderedList.add(
                ReliefRequestResponseDTO.fromEntity(polled, rank++)
        );
    }

    return orderedList;
}

/**
 * Comprehensive triage queue overview including aggregate deficits
 * and top priority item.
 */
@Transactional(readOnly = true)
public QueueSummaryDTO getPriorityQueueSummary() {

    List<ReliefRequest> all =
            reliefRequestRepository
                    .findAllByOrderByPriorityScoreDescRequestArrivalOrderAsc();

    List<ReliefRequestResponseDTO> activeQueue =
            getActivePriorityQueue();

    QueueSummaryDTO summary = new QueueSummaryDTO();

    summary.setTotalRequests(all.size());
    summary.setActiveQueueDemands(activeQueue.size());

    int pending = 0;
    int partial = 0;
    int allocated = 0;

    int totalReq = 0;
    int totalAlloc = 0;
    int totalDeficit = 0;

    for (ReliefRequest r : all) {

        String status = r.getRequestStatus();

        if (STATUS_PENDING.equalsIgnoreCase(status)) {
            pending++;
        } else if (STATUS_PARTIALLY_ALLOCATED.equalsIgnoreCase(status)) {
            partial++;
        } else if (STATUS_ALLOCATED.equalsIgnoreCase(status)) {
            allocated++;
        }

        // Safely handle nullable quantities.
        Integer reqQty = r.getRequiredQuantity();
        Integer allocQty = r.getAllocatedQuantity();

        int req = 0;
        int alloc = 0;

        if (reqQty != null) {
            req = reqQty;
        }

        if (allocQty != null) {
            alloc = allocQty;
        }

        totalReq += req;
        totalAlloc += alloc;
        totalDeficit += Math.max(0, req - alloc);
    }

    summary.setPendingCount(pending);
    summary.setPartiallyAllocatedCount(partial);
    summary.setAllocatedCount(allocated);

    summary.setTotalRequiredUnits(totalReq);
    summary.setTotalAllocatedUnits(totalAlloc);
    summary.setTotalRemainingDeficit(totalDeficit);

    summary.setTopPriorityRequest(
            activeQueue.isEmpty() ? null : activeQueue.get(0)
    );

    summary.setActiveQueue(activeQueue);

    return summary;
}

/**
 * Retrieve a specific relief request by ID.
 */
@Transactional(readOnly = true)
public ReliefRequestResponseDTO getRequestById(Integer id) {

    ReliefRequest request =
            reliefRequestRepository.findById(id)
                    .orElseThrow(() ->
                            new ResourceNotFoundException(
                                    "Relief request #" + id + " not found."
                            )
                    );

    Map<Integer, Integer> queueRanks =
            computeActiveQueueRanks(
                    reliefRequestRepository
                            .findAllByOrderByPriorityScoreDescRequestArrivalOrderAsc()
            );

    return ReliefRequestResponseDTO.fromEntity(
            request,
            queueRanks.get(id)
    );
}

/**
 * Retrieve all relief requests for a designated disaster region.
 */
@Transactional(readOnly = true)
public List<ReliefRequestResponseDTO> getRequestsByRegion(Integer regionId) {

    if (!affectedRegionRepository.existsById(regionId)) {
        throw new ResourceNotFoundException(
                "Disaster region with ID " + regionId + " does not exist."
        );
    }

    List<ReliefRequest> regionRequests =
            reliefRequestRepository
                    .findByRegionRegionIdOrderByPriorityScoreDescRequestArrivalOrderAsc(
                            regionId
                    );

    Map<Integer, Integer> queueRanks =
            computeActiveQueueRanks(
                    reliefRequestRepository
                            .findAllByOrderByPriorityScoreDescRequestArrivalOrderAsc()
            );

    return regionRequests.stream()
            .map(r -> ReliefRequestResponseDTO.fromEntity(
                    r,
                    queueRanks.get(r.getRequestId())
            ))
            .collect(Collectors.toList());
}

/**
 * Register a new relief demand with automated regional priority score
 * inheritance and monotonic FIFO arrival sequence assignment.
 */
@Transactional
public synchronized ReliefRequestResponseDTO createReliefRequest(
        ReliefRequestCreateDTO createDTO) {

    AffectedRegion region =
            affectedRegionRepository.findById(createDTO.getRegionId())
                    .orElseThrow(() ->
                            new ResourceNotFoundException(
                                    "Disaster region with ID "
                                            + createDTO.getRegionId()
                                            + " does not exist."
                            )
                    );

    if (createDTO.getRequiredQuantity() == null
            || createDTO.getRequiredQuantity() <= 0) {

        throw new InvalidRegionDataException(
                "Required quantity must be strictly greater than zero."
        );
    }

    String resourceNameClean =
            capitalizeWords(createDTO.getResourceRequired().trim());

    if (resourceNameClean.isEmpty()) {
        throw new InvalidRegionDataException(
                "Resource required must not be blank."
        );
    }

    // Inherit current priority score from target affected region.
    double priorityScore = region.getPriorityScore();

    // Safely determine next arrival order sequence.
    int nextArrivalOrder =
            reliefRequestRepository.findMaxArrivalOrder() + 1;

    ReliefRequest entity = new ReliefRequest();

    entity.setRegion(region);
    entity.setResourceRequired(resourceNameClean);
    entity.setRequiredQuantity(createDTO.getRequiredQuantity());
    entity.setAllocatedQuantity(0);
    entity.setPriorityScore(priorityScore);
    entity.setRequestArrivalOrder(nextArrivalOrder);
    entity.setRequestStatus(STATUS_PENDING);

    ReliefRequest saved =
            reliefRequestRepository.save(entity);

    // Compute updated queue rank.
    List<ReliefRequestResponseDTO> activeQueue =
            getActivePriorityQueue();

    Integer rank = null;

    for (ReliefRequestResponseDTO dto : activeQueue) {

        if (dto.getRequestId().equals(saved.getRequestId())) {
            rank = dto.getQueueRank();
            break;
        }
    }

    return ReliefRequestResponseDTO.fromEntity(saved, rank);
}

/**
 * Delete a relief request if no historical allocations reference it.
 */
@Transactional
public void deleteReliefRequest(Integer id) {

    ReliefRequest request =
            reliefRequestRepository.findById(id)
                    .orElseThrow(() ->
                            new ResourceNotFoundException(
                                    "Relief request #" + id + " not found."
                            )
                    );

    // Protect historical allocations.
    if (allocationRepository.existsByRequestRequestId(id)) {

        throw new ConflictException(
                "Cannot delete relief request #" + id
                        + " for sector '"
                        + request.getRegion().getRegionName()
                        + "' because active resource allocations reference this demand. "
                        + "Deleting it would violate relational foreign-key integrity "
                        + "and compromise disaster allocation history."
        );
    }

    reliefRequestRepository.delete(request);
}

/**
 * Helper to compute 1-based queue ranks for active requests using PriorityQueue.
 */
private Map<Integer, Integer> computeActiveQueueRanks(
        List<ReliefRequest> all) {

    PriorityQueue<ReliefRequest> pq =
            new PriorityQueue<>(PRIORITY_COMPARATOR);

    for (ReliefRequest r : all) {

        if (!STATUS_ALLOCATED.equalsIgnoreCase(r.getRequestStatus())) {
            pq.add(r);
        }
    }

    Map<Integer, Integer> ranks = new HashMap<>();

    int rank = 1;

    while (!pq.isEmpty()) {
        ranks.put(pq.poll().getRequestId(), rank++);
    }

    return ranks;
}

private String capitalizeWords(String str) {

    if (str == null || str.isEmpty()) {
        return str;
    }

    String[] words = str.split("\\s+");

    StringBuilder sb = new StringBuilder();

    for (String w : words) {

        if (!w.isEmpty()) {

            sb.append(Character.toUpperCase(w.charAt(0)));

            if (w.length() > 1) {
                sb.append(w.substring(1).toLowerCase());
            }

            sb.append(" ");
        }
    }

    return sb.toString().trim();
}

}
