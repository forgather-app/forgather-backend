import styled from '@emotion/styled';

export const Title = styled.h1`
  width: 100%;
  ${({ theme }) => ({
    ...theme.typography.header02,
  })}
`;
