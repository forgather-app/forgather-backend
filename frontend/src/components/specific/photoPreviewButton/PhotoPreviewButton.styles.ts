import styled from '@emotion/styled';
import { hexToRgba } from '../../../utils/hexToRgba';

export const Wrapper = styled.div`
  display: flex;
  flex-direction: column;
  align-items: center;
`;

export const Label = styled.label`
  display: flex;
  flex-direction: column;
  gap: 4px;
  width: 60px;
  aspect-ratio: 1/1;
  cursor: pointer;
  position: relative;
`;

export const Overlay = styled.div<{ isPhotoExist: boolean }>`
  position: absolute;
  top: 0;
  left: 0;
  background: ${({ theme, isPhotoExist }) =>
    isPhotoExist
      ? hexToRgba(theme.colors.gray06, 0.1)
      : hexToRgba(theme.colors.gray06, 0.3)};
  z-index: 1000;
  width: 100%;
  height: 100%;
  border-radius: 16px;
  display: flex;
  justify-content: center;
  align-items: center;

  svg {
    width: 32px;
    height: 32px;
    color: ${({ theme, isPhotoExist }) =>
      isPhotoExist ? hexToRgba(theme.colors.white, 0.5) : theme.colors.white};
    opacity: 0.8;

    &:active {
      scale: 0.95;
    }
  }
`;

export const FileInput = styled.input`
  display: none;
`;
