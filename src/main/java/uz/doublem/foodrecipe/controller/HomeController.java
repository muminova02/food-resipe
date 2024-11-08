package uz.doublem.foodrecipe.controller;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import uz.doublem.foodrecipe.entity.User;
import uz.doublem.foodrecipe.payload.ResponseMessage;
import uz.doublem.foodrecipe.repository.UserRepository;
import uz.doublem.foodrecipe.service.HomeService;

@RestController
@RequiredArgsConstructor
@RequestMapping("/home")
public class HomeController {
    private final HomeService homeService;
    private final UserRepository userRepository;
    @GetMapping()
    @Transactional
    public ResponseEntity<?> home(){
        User principal =(User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        String email = principal.getEmail();
        User user = userRepository.findByEmail(email).orElseThrow(() -> new RuntimeException("User not found!"));
        ResponseMessage responseMessage = homeService.homePage(user);
        return ResponseEntity.status(responseMessage.getStatus()?200:400).body(responseMessage);
    }
    @GetMapping("/category/{id}/recipes")
    public ResponseEntity<?> getRecipesByCategory(@PathVariable Integer id){
        ResponseMessage responseMessage = homeService.getRecipesByCategoryId(id);
        return ResponseEntity.status(responseMessage.getStatus()?200:400).body(responseMessage);
    }
}
