package com.spark.campusday.meal.supplier.model;

import lombok.Getter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.Locale;

@Getter
@Document(collection = "meal_suppliers")
public class Supplier {
    @Id private String id;
    private String name;
    @Indexed(unique = true) private String nameKey;
    private String contact;
    private String note;
    @CreatedDate private Instant createdAt;
    @LastModifiedDate private Instant updatedAt;

    protected Supplier() { }

    public Supplier(String name, String contact, String note) {
        revise(name, contact, note);
    }

    public void revise(String name, String contact, String note) {
        this.name = name.trim();
        this.nameKey = name.trim().toLowerCase(Locale.ROOT);
        this.contact = contact == null || contact.isBlank() ? null : contact.trim();
        this.note = note == null || note.isBlank() ? null : note.trim();
    }
}
