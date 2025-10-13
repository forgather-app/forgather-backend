import { useEffect, useRef } from 'react';
import { useSwiper } from 'swiper/react';

interface SwiperUpdaterProps {
  initialImageLength: number;
  newActiveIndex: number;
}

const SwiperUpdater = ({
  initialImageLength,
  newActiveIndex,
}: SwiperUpdaterProps) => {
  const swiper = useSwiper();
  const prevImageLength = useRef(initialImageLength);

  useEffect(() => {
    if (prevImageLength.current === initialImageLength) return;

    swiper.update();
    if (newActiveIndex >= 0 && newActiveIndex < initialImageLength) {
      swiper.slideTo(newActiveIndex, 0);
    }
    prevImageLength.current = initialImageLength;
  }, [initialImageLength, newActiveIndex, swiper]);

  return null;
};

export default SwiperUpdater;
