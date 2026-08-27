import { RouterProvider } from 'react-router-dom';
import { router } from './app/router';
import { DirectCallProvider } from './features/directChat/calls/DirectCallProvider';

export default function App() {
  return (
    <DirectCallProvider>
      <RouterProvider router={router} />
    </DirectCallProvider>
  );
}
