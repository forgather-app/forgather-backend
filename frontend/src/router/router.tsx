import { createBrowserRouter } from 'react-router-dom';
import Layout from '../components/layout/global/layout/Layout';
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
        handle: {
          highlight: false,
        },
      },
      {
        path: 'host',
        children: [
          {
            path: 'main',
            element: <HostMainPage />,
          },
        ],
      },
    ],
  },
];

const router = createBrowserRouter(routes);

export default router;
