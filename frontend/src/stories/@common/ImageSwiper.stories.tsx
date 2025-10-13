import type { Meta, StoryObj } from '@storybook/react';
import ImageSwiper from '../../components/@common/imageSwiper/ImageSwiper';

const meta: Meta<typeof ImageSwiper> = {
  title: 'Components/Swiper/ImageSwiper',
  component: ImageSwiper,
};
export default meta;

type Story = StoryObj<typeof meta>;

export const Default: Story = {
  args: {
    imageInfo: [
      { src: 'https://picsum.photos/200/300?random=1`', alt: 'image1' },
      { src: 'https://picsum.photos/200/300?random=2', alt: 'image2' },
      { src: 'https://picsum.photos/200/300?random=3', alt: 'image3' },
      { src: 'https://picsum.photos/200/300?random=4', alt: 'image4' },
      { src: 'https://picsum.photos/200/300?random=5', alt: 'image5' },
      { src: 'https://picsum.photos/200/300?random=6', alt: 'image6' },
      { src: 'https://picsum.photos/200/300?random=7', alt: 'image7' },
      { src: 'https://picsum.photos/200/300?random=8', alt: 'image8' },
    ],
  },
};
