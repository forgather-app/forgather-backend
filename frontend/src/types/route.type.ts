import type { NonIndexRouteObject } from 'react-router-dom';

export type LeftIcons = 'logo' | 'profile' | 'back';

export type RouteHandle = {
  starField?: boolean;
  highlight?: boolean;
  noHamburger?: boolean;
  headerIcon?: {
    leftIcon?: LeftIcons;
  };
  noHeader?: boolean;
  noFooter?: boolean;
};

export interface IconAction {
  icon: React.ReactNode;
  onClick?: () => void;
}

export interface NavigateInfo {
  path: string;
  name: string;
}

export interface AppRouteObject extends NonIndexRouteObject {
  handle?: RouteHandle;
  children?: AppRouteObject[];
}
