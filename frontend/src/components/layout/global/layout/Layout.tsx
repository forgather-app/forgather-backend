import { IoSettingsSharp, IoShareOutline } from 'react-icons/io5';
import { Outlet, useMatches } from 'react-router-dom';
import OverlayProvider from '../../../../contexts/OverlayProvider';
import type { AppRouteObject } from '../../../../types/route.type';
import Header from '../../../@common/header/Header';
import * as S from './Layout.styles';

const Layout = () => {
  const headerIcons = {
    share: <IoShareOutline />,
    settings: <IoSettingsSharp />,
  };

  const matches = useMatches() as AppRouteObject[];
  const current = matches[matches.length - 1];
  const isDarkPage = current?.handle?.highlight;
  const matchedIcons = current?.handle?.headerIcons.map(
    (icon: keyof typeof headerIcons) => headerIcons[icon],
  );

  return (
    <OverlayProvider>
      <Header mode={isDarkPage ? 'dark' : 'light'} icons={matchedIcons} />
      <S.Container $isDarkPage={isDarkPage}>
        <Outlet />
      </S.Container>
    </OverlayProvider>
  );
};

export default Layout;
