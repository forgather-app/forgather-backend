import { EffectCoverflow, Navigation, Pagination } from 'swiper/modules';
import { Swiper, SwiperSlide } from 'swiper/react';
import 'swiper/css';
import * as S from './ImageSwiper.styles';
import 'swiper/css/navigation';
import 'swiper/css/pagination';

interface ImageInfoType {
  src: string;
  alt: string;
}

interface ImageSwiperProps {
  imageInfo: ImageInfoType[];
}

const ImageSwiper = ({ imageInfo }: ImageSwiperProps) => {
  return (
    <S.ImageSwiperContainer>
      <Swiper
        slidesPerView="auto"
        pagination={{ clickable: true, type: 'fraction' }}
        slidesOffsetBefore={-10}
        slidesOffsetAfter={-10}
        modules={[Navigation, Pagination, EffectCoverflow]}
        centeredSlides={true}
        grabCursor
        loop
        effect="coverflow"
        coverflowEffect={{
          rotate: 0,
          stretch: -40,
          depth: 200,
          slideShadows: true,
        }}
      >
        {imageInfo.map((imageInfo: ImageInfoType) => (
          <SwiperSlide key={imageInfo.src} style={{ display: 'flex' }}>
            <img src={imageInfo.src} alt={imageInfo.alt} />
          </SwiperSlide>
        ))}
      </Swiper>
    </S.ImageSwiperContainer>
  );
};

export default ImageSwiper;
