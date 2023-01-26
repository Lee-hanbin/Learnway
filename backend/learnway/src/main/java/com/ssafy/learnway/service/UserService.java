package com.ssafy.learnway.service;

import com.ssafy.learnway.domain.Language;
import com.ssafy.learnway.domain.RefreshToken;
import com.ssafy.learnway.domain.user.User;
import com.ssafy.learnway.domain.user.UserInterest;
import com.ssafy.learnway.dto.*;
import com.ssafy.learnway.exception.TokenValidFailedException;
import com.ssafy.learnway.exception.UserNotFoundException;
import com.ssafy.learnway.repository.*;
import com.ssafy.learnway.util.JwtTokenProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

@Service
public class UserService {
    @Autowired
    private UserRepository userRepository;


    @Autowired
    private UserInterestRepository userInterestRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Transactional
    public TokenDto login(String userEmail, String userPwd) throws SQLException {

        User user = userRepository.findByUserEmail(userEmail);

        // 장소에서 가져온 인코딩된 암호(encodedPassword)가 인코딩 된 후 제출된 원시 암호(raw password)와 일치하는지 확인
        // 불일치하면 확인 msg 반환.
        if (user == null || !passwordEncoder.matches(userPwd, user.getPassword()) ) {
            throw new SQLException();
        }

        // 유저 정보와 유저 권한이 담긴 token 생성. UserNamePasswordAuthentication Token 생성
        // String token = jwtTokenProvider.createToken(String.valueOf(user.getUserId()), user.getRoles());

        // AccessToken, RefreshToken 발급
        TokenDto tokenDto = jwtTokenProvider.createTokenDto(user.getUserId(),user.getRoles());

        RefreshToken refreshToken = RefreshToken.builder()
                .userKey(user.getUserId())
                .token(tokenDto.getRefreshToken())
                .build();

        refreshTokenRepository.save(refreshToken);

        return tokenDto;
    }
    @Transactional
    public UserDto userInfo (String userEmail){
        User user = userRepository.findByUserEmail(userEmail);

        Language language = user.getLanguageId();
        LanguageDto languageDto = LanguageDto.builder().languageId(language.getLanguageId()).name(language.getName()).build();

        List<UserInterest> userInterests = userInterestRepository.findAllByUserId(user);

        List<InterestDto> interests = new ArrayList<>();
        for(UserInterest userInterest : userInterests){
            InterestDto interestDto = InterestDto.builder()
                    .interestId(userInterest.getInterestId().getInterestId())
                    .field(userInterest.getInterestId().getField()).build();
            interests.add(interestDto);
        }

        UserDto userDto = UserDto.builder()
                .userEmail(user.getUserEmail())
                .name(user.getName())
                .birthDay(user.getBirthday())
                .language(languageDto)
                .interests(interests)
                .badUser(user.isBadUser())
                .imgUrl(user.getImgUrl())
                .userId(user.getUserId())
                .bio(user.getBio()).build();
        return userDto;
    }

    @Transactional
    public UserDto userInfoPwd (String userEmail){
        User user = userRepository.findByUserEmail(userEmail);

        Language language = user.getLanguageId();
        LanguageDto languageDto = LanguageDto.builder().languageId(language.getLanguageId()).name(language.getName()).build();

        List<UserInterest> userInterests = userInterestRepository.findAllByUserId(user);

        List<InterestDto> interests = new ArrayList<>();
        for(UserInterest userInterest : userInterests){
            InterestDto interestDto = InterestDto.builder()
                    .interestId(userInterest.getInterestId().getInterestId())
                    .field(userInterest.getInterestId().getField()).build();
            interests.add(interestDto);
        }

        UserDto userDto = UserDto.builder()
                .userEmail(user.getUserEmail())
                .userPwd(user.getUserPwd())
                .name(user.getName())
                .birthDay(user.getBirthday())
                .language(languageDto)
                .interests(interests)
                .badUser(user.isBadUser())
                .imgUrl(user.getImgUrl())
                .bio(user.getBio())
                .userId(user.getUserId())
                .build();
        return userDto;
    }

    @Transactional
    public void signUp(UserDto userDto) throws SQLException {

        if(userRepository.findByUserEmail(userDto.getUserEmail())==null){
            User user = userRepository.save(userDto.toEntity());

            for(InterestDto interestDto : userDto.getInterests()){
                UserInterest userInterest = UserInterest.builder()
                                .userId(user).interestId(interestDto.toEntity()).build();

                userInterestRepository.save(userInterest);

            }
        }
        else throw new SQLException();
    }
    @Transactional
    public User findByEmail(String userEmail) throws SQLException{
        return userRepository.findByUserEmail(userEmail);
    }

    @Transactional
    public TokenDto refreshToken(TokenRequestDto tokenRequestDto) throws SQLException {

        // 만료된 refresh token 에러
        if(!jwtTokenProvider.validateToken(tokenRequestDto.getRefreshToken())){
            System.out.println("토큰 에러");
            throw new TokenValidFailedException(); // refreshtoken 예외 처리로 바꿔주기
        }

        // AccessToken 에서 username(pk) 가져오기
        String accessToken = tokenRequestDto.getAccessToken();
        Authentication authentication = jwtTokenProvider.getAuthentication(accessToken);

        // userPk로 유저 검섹
        // User user = userRepository.findById(Long.parseLong(authentication.getName())).orElseThrow(() -> new Exception());// usernotfound 예외 처리로 바꿔주기
        User user = userRepository.findByUserEmail(authentication.getName());
        if (user == null ) {
            throw new UserNotFoundException();
        }
        RefreshToken refreshToken = refreshTokenRepository.findByUserKey(user.getUserId());

        // 전달받은 refreshToken과 db의 refreshToken 비교
        if(!refreshToken.getToken().equals(tokenRequestDto.getRefreshToken()))
            throw new TokenValidFailedException();// refreshtoken 예외 처리로 바꿔주기

        // AccessToken, RefreshToken 재발급 및 RefreshToken 저장
        TokenDto newCreatedToken = jwtTokenProvider.createTokenDto(user.getUserId(), user.getRoles());
        RefreshToken updateRefreshToken  = refreshToken.updateToken(newCreatedToken.getRefreshToken());
        refreshTokenRepository.save(updateRefreshToken);

        return newCreatedToken;
    }

    public User dupName(String name){
        return userRepository.findByName(name);
    }

    public void userModify(UserDto userDto) {

        // 수정
        User user = userRepository.save(userDto.toEntity());

        userInterestRepository.deleteAllByUserId(user);

        for(InterestDto interestDto : userDto.getInterests()){
            UserInterest userInterest = UserInterest.builder()
                    .userId(user).interestId(interestDto.toEntity()).build();
            userInterestRepository.save(userInterest);
        }

    }
}
