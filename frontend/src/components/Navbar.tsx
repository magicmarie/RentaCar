import { Link, useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';

export function Navbar() {
  const { user, logout } = useAuth();
  const navigate = useNavigate();

  function handleLogout() {
    logout();
    navigate('/login');
  }

  return (
    <header className="navbar">
      <Link to="/" className="navbar-brand">RentaCar</Link>
      <nav className="navbar-links">
        {user?.role === 'CUSTOMER' && (
          <>
            <Link to="/search">Find a Vehicle</Link>
            <Link to="/my-reservations">My Reservations</Link>
            <Link to="/profile">Profile</Link>
          </>
        )}
        {user?.role === 'STAFF' && (
          <>
            <Link to="/staff/reservations">All Reservations</Link>
            <Link to="/staff/lookup">Reservation Lookup</Link>
          </>
        )}
        {user?.role === 'ADMIN' && (
          <>
            <Link to="/admin/dashboard">Dashboard</Link>
            <Link to="/admin/vehicles">Vehicles</Link>
            <Link to="/admin/categories">Categories</Link>
            <Link to="/admin/staff">Staff Accounts</Link>
          </>
        )}
      </nav>
      <div className="navbar-user">
        {user ? (
          <>
            <span className="navbar-username">{user.firstName} ({user.role})</span>
            <button type="button" className="btn btn-secondary" onClick={handleLogout}>
              Log out
            </button>
          </>
        ) : (
          <Link to="/login">Log in</Link>
        )}
      </div>
    </header>
  );
}
