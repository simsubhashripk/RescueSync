import React from 'react';
import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import { AuthProvider } from './context/AuthContext';
import ProtectedRoute from './components/ProtectedRoute';
import Navbar from './components/Navbar';
import HomePage from './pages/HomePage';
import Login from './pages/Login';
import Registration from './pages/Registration';
import RegionList from './pages/RegionList';
import RegionForm from './pages/RegionForm';
import RegionDetails from './pages/RegionDetails';
import ResourceDashboard from './pages/ResourceDashboard';
import ResourceForm from './pages/ResourceForm';
import ReliefRequestDashboard from './pages/ReliefRequestDashboard';
import PriorityQueueView from './pages/PriorityQueueView';
import ReliefRequestForm from './pages/ReliefRequestForm';
import AllocationDashboard from './pages/AllocationDashboard';
import RouteMapView from './pages/RouteMapView';
import RedistributionDashboard from './pages/RedistributionDashboard';
import AdminAnalyticsDashboard from './pages/AdminAnalyticsDashboard';
import './styles/App.css';

export default function App() {
  return (
    <BrowserRouter>
      <AuthProvider>
        <div className="app-container">
          <Navbar />
          <main className="content-area">
            <Routes>
              {/* Public Routes */}
              <Route path="/" element={<HomePage />} />
              <Route path="/login" element={<Login />} />
              <Route path="/register" element={<Registration />} />

              {/* Protected Disaster Management Routes */}
              <Route
                path="/regions"
                element={
                  <ProtectedRoute>
                    <RegionList />
                  </ProtectedRoute>
                }
              />
              <Route
                path="/regions/new"
                element={
                  <ProtectedRoute>
                    <RegionForm />
                  </ProtectedRoute>
                }
              />
              <Route
                path="/regions/:id"
                element={
                  <ProtectedRoute>
                    <RegionDetails />
                  </ProtectedRoute>
                }
              />
              <Route
                path="/regions/:id/edit"
                element={
                  <ProtectedRoute>
                    <RegionForm />
                  </ProtectedRoute>
                }
              />

              {/* Protected Resource Inventory Routes */}
              <Route
                path="/resources"
                element={
                  <ProtectedRoute>
                    <ResourceDashboard />
                  </ProtectedRoute>
                }
              />
              <Route
                path="/resources/new"
                element={
                  <ProtectedRoute>
                    <ResourceForm />
                  </ProtectedRoute>
                }
              />
              <Route
                path="/resources/:id/edit"
                element={
                  <ProtectedRoute>
                    <ResourceForm />
                  </ProtectedRoute>
                }
              />

              {/* Protected Relief Request & Priority Queue Routes */}
              <Route
                path="/requests"
                element={
                  <ProtectedRoute>
                    <ReliefRequestDashboard />
                  </ProtectedRoute>
                }
              />
              <Route
                path="/requests/queue"
                element={
                  <ProtectedRoute>
                    <PriorityQueueView />
                  </ProtectedRoute>
                }
              />
              <Route
                path="/requests/new"
                element={
                  <ProtectedRoute>
                    <ReliefRequestForm />
                  </ProtectedRoute>
                }
              />

              {/* Protected Resource Allocation Routes */}
              <Route
                path="/allocations"
                element={
                  <ProtectedRoute>
                    <AllocationDashboard />
                  </ProtectedRoute>
                }
              />

              {/* Protected Geospatial & Dijkstra Route Map */}
              <Route
                path="/map"
                element={
                  <ProtectedRoute>
                    <RouteMapView />
                  </ProtectedRoute>
                }
              />

              {/* Protected Dynamic Redistribution Routes */}
              <Route
                path="/redistribution"
                element={
                  <ProtectedRoute>
                    <RedistributionDashboard />
                  </ProtectedRoute>
                }
              />

              {/* Protected Admin & Operations Analytics */}
              <Route
                path="/analytics"
                element={
                  <ProtectedRoute>
                    <AdminAnalyticsDashboard />
                  </ProtectedRoute>
                }
              />

              <Route path="*" element={<Navigate to="/" replace />} />
            </Routes>
          </main>
          <footer className="footer">
            RescueSync | Smart Disaster Resource Allocation and Emergency Response
          </footer>
        </div>
      </AuthProvider>
    </BrowserRouter>
  );
}
