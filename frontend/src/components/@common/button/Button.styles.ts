import { css, type Theme } from '@emotion/react';
import styled from '@emotion/styled';
import type { ButtonVariant } from '../../../types/button.type';

export const buttonStyles = {
  primary: (theme: Theme) => css`
    border-radius: 4px;
    background-color: ${theme.colors.gray06};
    color: ${theme.colors.white};

    &:active {
      border-radius: 4px;
      background-color: ${theme.colors.gray06};
      box-shadow: 0px 2px 3px 0px rgba(0, 0, 0, 0.6) inset;
      color: ${theme.colors.gray02};
    }

    &:disabled {
      pointer-events: none;
      border-radius: 4px;
      background-color: ${theme.colors.gray02};
      color: ${theme.colors.gray03};
    }
  `,

  secondary: (theme: Theme) => css`
    border-radius: 4px;
    color: ${theme.colors.gray04};
    background-color: ${theme.colors.white};
    border: 1px solid ${theme.colors.gray04};

    &:disabled {
      border-radius: 4px;
      pointer-events: none;
      color: ${theme.colors.gray03};
      background-color: ${theme.colors.gray01};
      border: 1px solid ${theme.colors.gray03};
    }

    &:active {
      border-radius: 4px;
      background-color: ${theme.colors.gray01};
    }
  `,
};

export const StyledButton = styled.button<{
  $variant: ButtonVariant;
}>`
  width: 100%;
  display: flex;
  padding: 12px 20px;
  justify-content: center;
  align-items: center;
  gap: 4px;
  white-space: nowrap;

  ${({ theme }) => ({ ...theme.typography.button })}
  ${({ $variant, theme }) => buttonStyles[$variant](theme)}
`;

export const Icon = styled.div`
  display: flex;
  align-items: center;
  justify-content: center;
`;
