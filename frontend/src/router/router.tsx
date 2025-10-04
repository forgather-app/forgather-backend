import { createBrowserRouter } from 'react-router-dom';
import Layout from '../components/layout/global/layout/Layout';
import DashBoard from '../pages/host/dashBoard/DashBoard';
import HostMainPage from '../pages/host/mainPage/HostMainPage';
import MainPage from '../pages/MainPage';
import type { AppRouteObject } from '../types/route.type';

const routes: AppRouteObject[] = [
  {
    path: '/',
    element: <Layout />,
    children: [
      {
        path: '/',
        element: <MainPage />,
      },
      {
        path: 'host',
        children: [
          {
            path: 'main',
            element: <HostMainPage />,
            handle: {
              headerIcons: ['share', 'settings'],
            },
          },
          {
            path: 'dashboard',
            element: <DashBoard />,
            handle: {
              headerIcons: ['settings'],
            },
          },
        ],
      },
    ],
  },
];

const router = createBrowserRouter(routes);

export default router;
