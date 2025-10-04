import styled from '@emotion/styled';

export const Wrapper = styled.div`
width: 100%;
display: flex;
align-items: center;
justify-content: center;
`;

export const ProfileContainer = styled.section`
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 16px;
`;

export const Thumbnail = styled.img`
  max-width: 60px;
  width: 100%;
  aspect-ratio: 1 / 1;
  object-fit: cover;
  border-radius: 16px;
`;

export const InfoContainer = styled.div`
  display: flex;
  flex-direction: column;
  gap: 4px;
  text-align: center;
`;

export const Name = styled.h1`
  ${({ theme }) => theme.typography.header02}
`;

export const Introduction = styled.p`
  ${({ theme }) => theme.typography.bodyRegular}
  color: ${({ theme }) => theme.colors.gray04};
`;
