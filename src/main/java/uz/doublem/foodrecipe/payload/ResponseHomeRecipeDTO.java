package uz.doublem.foodrecipe.payload;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import uz.doublem.foodrecipe.entity.Recipe;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ResponseHomeRecipeDTO {
   private Integer id;
   private String title;
   private String cookingTime;
   private String imgUrl;
   private Double averageRating;
}
