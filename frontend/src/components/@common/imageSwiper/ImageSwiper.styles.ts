import styled from '@emotion/styled';

export const ImageSwiperContainer = styled.div`

  & .swiper {
    max-width: ${({ theme }) => theme.layout.width};
    max-height: 300px;
    padding-bottom: 40px;
    display: flex;
    justify-content: center;
    align-items: center;
    border: 1px solid blue;
  }
  & .swiper-slide {
    width: 45%;
    display: flex;
    justify-content: center;
    align-items: center; 
    transition: opacity 0.8s ease-in-out
  }
  & .swiper-slide-prev{
    opacity: 0.8;
  }
  & .swiper-slide-next{
    opacity: 0.8;
  }
  & .swiper-pagination {
    ${({ theme }) => ({ ...theme.typography.captionSmall })}
    color: ${({ theme }) => theme.colors.gray03};
  }
  & img{
    width: 100%;
    height: 100%;
    border-radius: 4px;
    object-fit: cover;
  }
`;
