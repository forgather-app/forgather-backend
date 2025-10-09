import { useId } from 'react';
import { IoCamera } from 'react-icons/io5';
import defaultImage from '../../../@assets/images/default-image.png';
import { Thumbnail } from '../../../pages/MainPage.common.styles';
import type { PreviewFile } from '../../../types/file.type';
import { createImageErrorHandler } from '../../../utils/createImageErrorHandler';
import Button from '../../@common/buttons/button/Button';
import * as S from './PhotoPreviewButton.styles';

interface PhotoPreviewButtonProps
  extends React.ButtonHTMLAttributes<HTMLButtonElement> {
  uploadImage: (event: React.ChangeEvent<HTMLInputElement>) => void;
  previewFile: PreviewFile[];
  originalSrc?: string;
}

const PhotoPreviewButton = ({
  uploadImage,
  originalSrc,
  previewFile,
}: PhotoPreviewButtonProps) => {
  const fileInputId = useId();

  const matchThumbnailImage = () =>
    previewFile[0]?.previewUrl || originalSrc || defaultImage;

  return (
    <S.Wrapper>
      <S.Label htmlFor={fileInputId}>
        <Thumbnail
          src={matchThumbnailImage()}
          onError={createImageErrorHandler(defaultImage)}
        />
        <S.Overlay isPhotoExist={!!previewFile[0]?.previewUrl}>
          <IoCamera />
        </S.Overlay>
      </S.Label>
      <S.FileInput
        id={fileInputId}
        type="file"
        accept="image/*"
        onChange={uploadImage}
      />
      <Button
        type="button"
        variant="tertiary"
        text="사진 삭제"
        onClick={() => {}}
      />
    </S.Wrapper>
  );
};

export default PhotoPreviewButton;
