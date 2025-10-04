import styled from '@emotion/styled';

export const Container = styled.main<{ $isDarkPage: boolean }>`
  position: relative;
  margin: 0 auto;
  max-width: ${({ theme }) => theme.layout.width};
  width: 100%;
  padding: ${({ theme }) => `${theme.layout.padding.topBottom} ${theme.layout.padding.leftRight}`};
  min-height: 100dvh;
  background: ${({ theme, $isDarkPage }) =>
    $isDarkPage ? theme.colors.gray06 : theme.colors.background};
`;
