import styled from '@emotion/styled';

export const NoImageContainer = styled.div`
  width: 100%;
  height: 100%;
  display: flex;
  justify-content: center;
  align-items: center;
`;

export const NoImageComment = styled.h2`
  ${({ theme }) => ({ ...theme.typography.bodyLarge })}
  color: ${({ theme }) => theme.colors.gray03};
  text-align: center;
`;

export const ImageSwiperContainer = styled.div`
  & .swiper {
    max-width: ${({ theme }) => theme.layout.width};
    max-height: 300px;
    height: fit-content;
    padding-bottom: 30px;
    display: flex;
    justify-content: center;
    align-items: center;
    position: relative;
  }
  & .swiper-slide {
    width: 45%;
    height: 100%;
    display: flex;
    justify-content: center;
    align-items: center;
    transition: opacity 0.8s ease-in-out;
    border-radius: 4px;
  }
  & .swiper-slide-prev img {
    opacity: 0.8;
  }
  & .swiper-slide-next img {
    opacity: 0.8;
  }
  & .swiper-pagination {
    ${({ theme }) => ({ ...theme.typography.captionSmall })}
    color: ${({ theme }) => theme.colors.gray04};
    position: absolute;
    bottom: 0;
  }
  & img {
    width: 100%;
    height: 100%;
    border-radius: 4px;
    object-fit: cover;
  }
`;
