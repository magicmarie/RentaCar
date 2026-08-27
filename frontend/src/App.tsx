import { Navigate, Route, Routes } from 'react-router-dom';
import { Navbar } from './components/Navbar';
import { ProtectedRoute } from './routes/ProtectedRoute';
import { useAuth } from './context/AuthContext';

import { LoginPage } from './pages/auth/LoginPage';
import { RegisterPage } from './pages/auth/RegisterPage';
import { ForgotPasswordPage } from './pages/auth/ForgotPasswordPage';
import { ResetPasswordPage } from './pages/auth/ResetPasswordPage';

import { SearchVehiclesPage } from './pages/customer/SearchVehiclesPage';
import { ReservationHistoryPage } from './pages/customer/ReservationHistoryPage';
import { ProfilePage } from './pages/customer/ProfilePage';

import { AllReservationsPage } from './pages/staff/AllReservationsPage';
import { ReservationLookupPage } from './pages/staff/ReservationLookupPage';
import { CheckOutPage } from './pages/staff/CheckOutPage';
import { CheckInPage } from './pages/staff/CheckInPage';

import { VehiclesAdminPage } from './pages/admin/VehiclesAdminPage';
import { CategoriesAdminPage } from './pages/admin/CategoriesAdminPage';
import { StaffAccountsAdminPage } from './pages/admin/StaffAccountsAdminPage';
import { DashboardAdminPage } from './pages/admin/DashboardAdminPage';

const ROLE_HOME: Record<string, string> = {
  ADMIN: '/admin/dashboard',
  STAFF: '/staff/lookup',
  CUSTOMER: '/search',
};

function HomeRedirect() {
  const { user } = useAuth();
  return <Navigate to={user ? (ROLE_HOME[user.role] ?? '/login') : '/login'} replace />;
}

function App() {
  return (
    <>
      <Navbar />
      <main>
        <Routes>
          <Route path="/" element={<HomeRedirect />} />
          <Route path="/login" element={<LoginPage />} />
          <Route path="/register" element={<RegisterPage />} />
          <Route path="/forgot-password" element={<ForgotPasswordPage />} />
          <Route path="/reset-password" element={<ResetPasswordPage />} />

          <Route path="/search" element={
            <ProtectedRoute allowedRoles={['CUSTOMER']}><SearchVehiclesPage /></ProtectedRoute>
          } />
          <Route path="/my-reservations" element={
            <ProtectedRoute allowedRoles={['CUSTOMER']}><ReservationHistoryPage /></ProtectedRoute>
          } />
          <Route path="/profile" element={
            <ProtectedRoute allowedRoles={['CUSTOMER']}><ProfilePage /></ProtectedRoute>
          } />

          <Route path="/staff/reservations" element={
            <ProtectedRoute allowedRoles={['STAFF']}><AllReservationsPage /></ProtectedRoute>
          } />
          <Route path="/staff/lookup" element={
            <ProtectedRoute allowedRoles={['STAFF']}><ReservationLookupPage /></ProtectedRoute>
          } />
          <Route path="/staff/checkout/:id" element={
            <ProtectedRoute allowedRoles={['STAFF']}><CheckOutPage /></ProtectedRoute>
          } />
          <Route path="/staff/checkin/:id" element={
            <ProtectedRoute allowedRoles={['STAFF']}><CheckInPage /></ProtectedRoute>
          } />

          <Route path="/admin/dashboard" element={
            <ProtectedRoute allowedRoles={['ADMIN']}><DashboardAdminPage /></ProtectedRoute>
          } />
          <Route path="/admin/vehicles" element={
            <ProtectedRoute allowedRoles={['ADMIN']}><VehiclesAdminPage /></ProtectedRoute>
          } />
          <Route path="/admin/categories" element={
            <ProtectedRoute allowedRoles={['ADMIN']}><CategoriesAdminPage /></ProtectedRoute>
          } />
          <Route path="/admin/staff" element={
            <ProtectedRoute allowedRoles={['ADMIN']}><StaffAccountsAdminPage /></ProtectedRoute>
          } />

          <Route path="*" element={<Navigate to="/" replace />} />
        </Routes>
      </main>
    </>
  );
}

export default App;
