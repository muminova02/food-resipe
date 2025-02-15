package uz.doublem.foodrecipe.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uz.doublem.foodrecipe.entity.Recipe;
import uz.doublem.foodrecipe.entity.SavedRecipes;
import uz.doublem.foodrecipe.entity.User;
import uz.doublem.foodrecipe.payload.RecipeId;
import uz.doublem.foodrecipe.payload.ResponseMessage;
import uz.doublem.foodrecipe.payload.SavedResponseDto;
import uz.doublem.foodrecipe.repository.RecipeRepositoryM;
import uz.doublem.foodrecipe.repository.SavedRecipesRepository;
import uz.doublem.foodrecipe.util.Util;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SavedRecipeService {


    private  final SavedRecipesRepository savedReciepe;
    private final RecipeRepositoryM recipeRepository;
    private final NotificationService notificationService;



    public ResponseMessage getSavedRecipes(User user, Integer size, Integer page) {
        PageRequest pageRequest = PageRequest.of(page, size);
        Page<SavedRecipes> allByOwnerId = savedReciepe.findAllByOwner_Id(user.getId(), pageRequest);

        List<SavedResponseDto> list = allByOwnerId.get().map(savedRecipes -> {
            Recipe recipe = savedRecipes.getRecipe();
            return SavedResponseDto.builder()
                    .id(recipe.getId())
                    .author(recipe.getAuthor().getName())
                    .averageRating(recipe.getAverageRating())
                    .title(recipe.getTitle())
                    .description(recipe.getDescription())
                    .time(recipe.getCookingTime())
                    .imageUrl(recipe.getImageUrl()).build();
        }).toList();
        return ResponseMessage.builder()
                .status(true)
                .text("saved recipe list")
                .data(list)
                .build();
    }

    @Transactional
    public ResponseMessage unSaveRecipe(Integer id, User user) {
            savedReciepe.findByOwner_IdAndRecipe_Id(user.getId(),id).ifPresent(savedReciepe::delete);
        return Util.getResponseMes(true,"unsave recipe successfully",id);
    }


    @Transactional
    public ResponseMessage createSavedRecipes(User user, RecipeId recipeId) {
        ResponseMessage responseMessage = new ResponseMessage();
        responseMessage.setStatus(false);
        responseMessage.setText("recipe not found "+recipeId);
        Integer id = recipeId.getId();
        if (!savedReciepe.existsByRecipe_IdAndOwner_Id(id,user.getId())) {
            recipeRepository.findById(id).ifPresent(recipe -> {
                SavedRecipes savedRecipe = new SavedRecipes();
                savedRecipe.setRecipe(recipe);
                savedRecipe.setOwner(user);
                savedReciepe.save(savedRecipe);
                responseMessage.setText("recipe saved for user ");
                responseMessage.setStatus(true);
//                notificationService.createNotificationForSavedRecipe(recipe,user);
            });
        }else {
            return unSaveRecipe(id, user);
        }
        return responseMessage;
    }
}
