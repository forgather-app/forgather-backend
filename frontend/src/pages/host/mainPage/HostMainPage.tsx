import { FaInstagram, FaRegEnvelope } from 'react-icons/fa';
import IconButton from '../../../components/@common/buttons/iconButton/IconButton';
import { getInstagramUrl } from '../../../utils/createExternalLinks';
import { mockData } from '../../mockData';
import * as S from './HostMainPage.styles';

const HostMainPage = () => {
  return (
    <S.Wrapper>
      <S.ProfileContainer>
        <S.Thumbnail src={mockData.thumbnail} />
        <S.InfoContainer>
          <S.Name>{mockData.title}</S.Name>
          <S.Introduction>{mockData.introduction}</S.Introduction>
        </S.InfoContainer>
      </S.ProfileContainer>
      <S.IconButtonContainer>
        <IconButton
          aria-label="인스타그램"
          icon={<FaInstagram />}
          variant="default"
          onClick={() =>
            window.open(getInstagramUrl(mockData.instagramId), '_blank')
          }
        />
        <IconButton
          aria-label="이메일"
          icon={<FaRegEnvelope />}
          variant="default"
          onClick={() => window.open(`mailto:${mockData.email}`, '_blank')}
        />
      </S.IconButtonContainer>
    </S.Wrapper>
  );
};

export default HostMainPage;
