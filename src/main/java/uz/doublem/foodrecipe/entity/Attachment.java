package uz.doublem.foodrecipe.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

@Entity
public class Attachment {
    @Id
    @GeneratedValue(strategy=GenerationType.IDENTITY)
    private int Integer;
}
