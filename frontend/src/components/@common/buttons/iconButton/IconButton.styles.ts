import { css, type Theme } from '@emotion/react';
import styled from '@emotion/styled';
import type { IconButtonVariant } from '../../../../types/button.type';
import { hexToRgba } from '../../../../utils/hexToRgba';

export const IconButtonStyles = {
  default: (theme: Theme) => css`
    color: ${theme.colors.gray06};
    padding: 0;
    &:active {
      scale: 0.95;
    }
    &:disabled {
      color: ${theme.colors.gray04};
    }
  `,
  outline: (theme: Theme) => css`
    border: 1px solid ${theme.colors.gray02};
    background-color: ${theme.colors.white};
    color: ${theme.colors.gray06};
  `,
  danger: (theme: Theme) => css`
    background: ${hexToRgba(theme.colors.white, 0.7)};
    color: ${theme.colors.error};
  `,
  dark: (theme: Theme) => css`
    background: ${hexToRgba(theme.colors.gray06, 0.7)};
    color: ${theme.colors.white};
  `,
};

export const IconContainer = styled.button<{
  $variant: IconButtonVariant;
}>`
  max-width: 44px;
  aspect-ratio: 1/1;
  display: flex;
  padding: 10px;
  overflow: hidden;
  flex-direction: row;
  justify-content: center;
  align-items: center;
  gap: 10px;
  border-radius: 12px;

  svg {
    width: 25px;
    height: 25px;
  }

  ${({ $variant, theme }) => $variant && IconButtonStyles[$variant](theme)}
`;
