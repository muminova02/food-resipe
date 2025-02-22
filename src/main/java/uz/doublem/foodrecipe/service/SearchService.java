package uz.doublem.foodrecipe.service;


import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import uz.doublem.foodrecipe.entity.RecentSearch;
import uz.doublem.foodrecipe.entity.Recipe;
import uz.doublem.foodrecipe.entity.User;
import uz.doublem.foodrecipe.payload.RecipeDtoShow;
import uz.doublem.foodrecipe.payload.ResponseMessage;
import uz.doublem.foodrecipe.payload.search.SearchDto;
import uz.doublem.foodrecipe.repository.RecipeRepositoryM;
import uz.doublem.foodrecipe.repository.ResentSearchRepository;
import uz.doublem.foodrecipe.repository.UserRepository;
import uz.doublem.foodrecipe.util.Util;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class SearchService {

    private final ResentSearchRepository resentSearchRepository;
    private final RecipeRepositoryM recipeRepositoryM;
    private final UserRepository userRepository;

    public ResponseMessage getResentSearch(Integer size, Integer page, User user) {
        PageRequest pageRequest = PageRequest.of(page, size);
//        if (!userRepository.existsById(userId)) {
//            return Util.getResponseMes(false,"User not found with this id",userId);
//        }
        Page<RecentSearch> allByUserId = resentSearchRepository.findAllByUser_Id(user.getId(), pageRequest);

        List<RecipeDtoShow> recipeDtoShowList = allByUserId.get().map(recentSearch -> {
                    Recipe recipe = recentSearch.getRecipe();
                    return RecipeDtoShow.builder()
                            .id(recipe.getId())
                            .title(recipe.getTitle())
                            .description(recipe.getDescription())
                            .imageUrl(recipe.getImageUrl())
                            .author(recipe.getAuthor().getName())
                            .averageRating(recipe.getAverageRating())
                            .build();
                })
                .toList();

        return ResponseMessage.builder()
                .data(recipeDtoShowList)
                .status(true)
                .text("This is the recent search recipe list page for user " + user.getId())
                .build();
    }

    public ResponseMessage getSearchResult(SearchDto searchDto, Integer size, Integer page) {
        PageRequest pageRequest = PageRequest.of(page, size);
        Page<Recipe> recipesPage = recipeRepositoryM.findAllBySearch(searchDto.getTitle(),searchDto.getRate(),searchDto.getTime(),searchDto.getCategory(),pageRequest);
        List<RecipeDtoShow> recipeDtoShowsList = recipesPage.get().map(recipe -> {
            return RecipeDtoShow.builder()
                    .id(recipe.getId())
                    .title(recipe.getTitle())
                    .description(recipe.getDescription())
                    .imageUrl(recipe.getImageUrl())
                    .averageRating(recipe.getAverageRating())
                    .author(recipe.getAuthor().getName())
                    .build();
        }).toList();
        return ResponseMessage.builder().data(recipeDtoShowsList).text("test").status(true).build();
    }

    public void createRecentSearch(Integer id, User user) {
        RecentSearch recentSearch = new RecentSearch();
        if (!userRepository.existsById(user.getId())&&!recipeRepositoryM.existsById(id)){
            throw new RuntimeException("user or recipe not found by id");
        }else {
            Optional<Recipe> byId = recipeRepositoryM.findById(id);
            if (byId.isPresent()) {
                recentSearch.setRecipe(byId.get());
                recentSearch.setUser(user);
            }
        }

        Optional<RecentSearch> byUserIdAndRecipeId = resentSearchRepository.findByUserIdAndRecipeId(user.getId(), id);
        if (byUserIdAndRecipeId.isPresent()){
            byUserIdAndRecipeId.ifPresent(resentSearchRepository::delete);
            resentSearchRepository.save(recentSearch);
        }else {
            resentSearchRepository.save(recentSearch);
        }

        long count = resentSearchRepository.count();
        if (count == 50) {
            Optional<RecentSearch> oldestSearch = resentSearchRepository.findFirstByUserIdOrderByCreatedAtAsc(user.getId());
            oldestSearch.ifPresent(resentSearchRepository::delete);
        }

    }
}
