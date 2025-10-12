export const handleKeyboardClick = (
  e: React.KeyboardEvent<HTMLElement>,
  callback?: () => void,
) => {
  if (e.key === 'Enter' || e.key === ' ') {
    e.preventDefault();
    if (callback) {
      callback();
      return;
    }
    e.currentTarget.click();
  }
};
