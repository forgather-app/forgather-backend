import { EffectCoverflow, Navigation, Pagination } from 'swiper/modules';
import { Swiper, SwiperSlide } from 'swiper/react';
import 'swiper/css';
import * as S from './ImageSwiper.styles';
import 'swiper/css/navigation';
import 'swiper/css/pagination';
import SwiperUpdater from '../../specific/swiper/SwiperUpdater';

interface ImageInfoType {
  src: string;
  alt: string;
}

interface ImageSwiperProps {
  /** 이미지 정보 */
  imageInfo: ImageInfoType[];
  /** 시작 인덱스 */
  initialSlide?: number;
  /** 초기 이미지 길이 */
  activeIndex: number;
  /** 현재 인덱스를 부모에게 전달할 함수 */
  onSlideChange: (index: number) => void;
}

const ImageSwiper = ({
  imageInfo,
  initialSlide = 0,
  activeIndex,
  onSlideChange,
}: ImageSwiperProps) => {
  if (imageInfo.length === 0)
    return (
      <S.NoImageContainer>
        <S.NoImageComment>선택된 이미지가 없습니다</S.NoImageComment>
      </S.NoImageContainer>
    );
  return (
    <S.ImageSwiperContainer>
      <Swiper
        /** swiper 스타일 설정 */
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
          slideShadows: false,
        }}
        /** swiper 제어 */
        initialSlide={initialSlide}
        onSlideChange={(swiper) => onSlideChange(swiper.realIndex)}
      >
        {imageInfo.map((imageInfo: ImageInfoType) => (
          <SwiperSlide key={imageInfo.src} style={{ display: 'flex' }}>
            <img src={imageInfo.src} alt={imageInfo.alt} />
          </SwiperSlide>
        ))}
        <SwiperUpdater
          newActiveIndex={activeIndex}
          initialImageLength={imageInfo.length}
        />
      </Swiper>
    </S.ImageSwiperContainer>
  );
};

export default ImageSwiper;
