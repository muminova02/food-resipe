package uz.doublem.foodrecipe.service;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.PostMapping;
import uz.doublem.foodrecipe.config.JwtProvider;
import uz.doublem.foodrecipe.entity.User;
import uz.doublem.foodrecipe.payload.ResetPasswordDTO;
import uz.doublem.foodrecipe.payload.ResponseMessage;
import uz.doublem.foodrecipe.payload.user.UserDTO;
import uz.doublem.foodrecipe.payload.user.UserSignInDTO;
import uz.doublem.foodrecipe.payload.user.UserVerifyDTO;
import uz.doublem.foodrecipe.repository.UserRepository;

import java.time.LocalDateTime;
@Component
@RequiredArgsConstructor
public class AuthService {
    private final UserRepository userRepository;
    private final SmsService smsService;
    private final PasswordEncoder passwordEncoder;
    private final JwtProvider jwtProvider;
    @SneakyThrows
    @PostMapping
    public ResponseMessage signUp(UserDTO userDTO){
        if (userRepository.existsByEmail(userDTO.email())) {
            return ResponseMessage.builder()
                    .status(false).
                    text("username already in use!")
                    .data(new RuntimeException()).build();
        }
        User user = new User();
        user.setName(userDTO.name());
        user.setEmail(userDTO.email());
        user.setPassword_hash(passwordEncoder.encode(userDTO.password()));
        user.setVerificationCodeGeneratedTime(LocalDateTime.now());
        String code = smsService.generateCode();
        user.setVerificationCode(code);
        user.setVerified(false);
        smsService.sendSmsToUser(userDTO.email(), code);
        userRepository.save(user);
        return ResponseMessage.builder()
                .status(true).
                text("confirm your account, check your mail")
                .data(userDTO).build();
    }

    public ResponseMessage signIn(UserSignInDTO userSignInDTO){
        User user = userRepository.findByEmail(userSignInDTO.getEmail()).orElseThrow(() -> new RuntimeException("user not found!"));
        if (!passwordEncoder.matches(userSignInDTO.getPassword(),user.getPassword_hash())){
            throw new RuntimeException("wrong password or username!");
        }
        if (!user.getVerified()){
                if (user.getVerificationCode()!= null){
                    return ResponseMessage.builder().status(false).data("error")
                            .text("Please verify your account first").build();
                }
            return ResponseMessage.builder().status(false).data(new RuntimeException())
                    .text("Please confirm your account first!").build();
        }
        String token = jwtProvider.generateToken(user);
        return ResponseMessage.builder().status(true).data(token).text("your token ").build();
    }

    public ResponseMessage verify(UserVerifyDTO userverifyDTO){
        LocalDateTime time = LocalDateTime.now();
        User user = userRepository.findByEmail(userverifyDTO.getEmail()).orElseThrow(() -> new RuntimeException("user not found!"));
        LocalDateTime generetedCodeTime = user.getVerificationCodeGeneratedTime();
        if (!user.getVerified()){
            if (user.getVerificationCode()==null) {
              return   ResponseMessage.builder()
                        .status(false)
                        .data("error")
                        .text("confimation code not found").build();
                            }
        }
        if (generetedCodeTime.plusMinutes(5).isBefore(time)){
            return   ResponseMessage.builder()
                    .status(false)
                    .data("error")
                    .text("confirmation time expired").build();
        }
        if (!user.getVerificationCode().equalsIgnoreCase(userverifyDTO.getCode())|| !user.getEmail().equalsIgnoreCase(userverifyDTO.getEmail())){
            return ResponseMessage.builder().status(false).data("error")
                    .text("incorrect code")
                    .build();
        }
        user.setVerified(true);
        userRepository.save(user);
        return ResponseMessage.builder().status(true).data(user).build();
    }


    public ResponseMessage requestPasswordReset(String email) {
        User user = userRepository.findByEmail(email).orElseThrow();
        String code = smsService.generateCode();
        user.setResetPasswordCode(code);
        user.setResetPasswordCodeGeneratedTime(LocalDateTime.now());
        userRepository.save(user);
        smsService.sendSmsToUser(email, smsService.generateCode());
        return ResponseMessage.builder().status(true).data(email).text("Reset code sent to your email").build();
    }

    public ResponseMessage resetPassword(ResetPasswordDTO resetPasswordDTO) {
        User user = userRepository.findByEmail(resetPasswordDTO.getEmail()).orElseThrow();
        if (user.getResetPasswordCodeGeneratedTime() == null) {
            throw new RuntimeException("No reset password code generated.");
        }
        if (!user.getResetPasswordCode().equals(resetPasswordDTO.getCode())) {
            throw new RuntimeException("Reset password code is incorrect.");
        }
        if (LocalDateTime.now().isAfter(user.getResetPasswordCodeGeneratedTime().plusMinutes(2))){
            throw new RuntimeException("time expired or verification code incorrect");
        }
        user.setPassword_hash(passwordEncoder.encode(resetPasswordDTO.getNewPassword()));
        userRepository.save(user);
        return ResponseMessage.builder().status(true).text("Password reset successfully").build();
    }
}
