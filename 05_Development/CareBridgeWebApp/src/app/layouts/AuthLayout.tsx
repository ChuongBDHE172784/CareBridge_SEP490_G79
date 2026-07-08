import { Outlet } from 'react-router-dom';

// Auth pages manage their own full-page layout and CareBridge design system styling.
export default function AuthLayout() {
  return <Outlet />;
}
