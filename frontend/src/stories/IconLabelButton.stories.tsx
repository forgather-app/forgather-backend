import type { Meta, StoryObj } from '@storybook/react';
import IconLabelButton from '../components/@common/buttons/iconLabelButton/IconLabelButton';
import { theme } from '../styles/theme';
import { FiLink, FiSave, FiTrash2 } from 'react-icons/fi';

const meta: Meta<typeof IconLabelButton> = {
  title: 'Components/IconLabelButton',
  component: IconLabelButton,
  parameters: {
    layout: 'centered',
  },
  argTypes: {
    icon: {
      control: false,
    },
    label: {
      control: 'text',
    },
  },
};
export default meta;

type Story = StoryObj<typeof meta>;

export const Default: Story = {
  args: {
    icon: <FiLink />,
    label: '링크 복사',
  },
};

export const NoLabel: Story = {
  args: {
    icon: <FiLink />,
  },
};

export const Dark: Story = {
  args: {
    icon: <FiLink />,
    variant: 'dark',
  },
};

export const Light: Story = {
  args: {
    icon: <FiSave color={theme.colors.white} />,
    variant: 'dark',
  },
};

export const Danger: Story = {
  args: {
    icon: <FiTrash2 color={theme.colors.error} size={16} />,
    variant: 'danger',
  },
  decorators: [
    (Story) => (
      <div
        style={{
          backgroundColor: theme.colors.gray02,
          padding: '40px',
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'center',
        }}
      >
        <Story />
      </div>
    ),
  ],
};
