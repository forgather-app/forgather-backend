import type { buttonStyles } from "../components/@common/buttons/button/Button.styles";
import type { IconLabelButtonStyles } from "../components/@common/buttons/iconLabelButton/IconLabelButton.styles";

export type ButtonVariant = keyof typeof buttonStyles;
export type IconLabelButtonVariant = keyof typeof IconLabelButtonStyles;