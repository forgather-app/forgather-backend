import AcUnitIcon from '@mui/icons-material/AcUnit';
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
    </S.Wrapper>
  );
};

export default HostMainPage;
