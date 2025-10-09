import { useEffect } from 'react';
import { useLocation } from 'react-router-dom';

const ScrollToTop = () => {
  const { pathname } = useLocation();

  // biome-ignore lint/correctness/useExhaustiveDependencies : pathname이 변경될 때마다 스크롤을 최상단으로 이동
  useEffect(() => {
    console.log('작동');
    window.scrollTo(0, 0);
  }, [pathname]);

  return null;
};

export default ScrollToTop;
