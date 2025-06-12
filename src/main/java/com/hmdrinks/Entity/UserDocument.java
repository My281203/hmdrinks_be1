package com.hmdrinks.Entity;



import com.hmdrinks.Enum.Role;
import com.hmdrinks.Enum.Sex;
import com.hmdrinks.Enum.TypeLogin;
import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import java.util.Date;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Document(indexName = "user")
public class UserDocument {

    @Id
    private Integer userId;
    @Field(type=FieldType.Text)
    private TypeLogin typeLogin;

    @Field(type = FieldType.Text)
    private String userName;

    @Field(type = FieldType.Text)
    private String email;

    @Field(type = FieldType.Keyword)
    private Role role;

    @Field(type = FieldType.Keyword)
    private Sex sex;

    @Field(type = FieldType.Text)
    private String fullName;

    @Field(type = FieldType.Text)
    private String phoneNumber;

    @Field(type = FieldType.Date)
    private Date birthDate;

    @Field(type = FieldType.Text)
    private String avatar;

    @Field(type = FieldType.Text)
    private String city;

    @Field(type = FieldType.Text)
    private String district;

    @Field(type = FieldType.Text)
    private String ward;

    @Field(type = FieldType.Text)
    private String street;

    @Field(type = FieldType.Boolean)
    private Boolean isDeleted;

    @Field(type = FieldType.Date)
    private Date dateDeleted;

    @Field(type = FieldType.Date)
    private Date dateUpdated;

    @Field(type = FieldType.Date)
    private Date dateCreated;

    public UserDocument(Integer userId, TypeLogin typeLogin,String userName, String email, Role role, Sex sex, String fullName, String phoneNumber, Date birthDate, String avatar, String city, String district, String ward, String street, Boolean isDeleted, Date dateUpdated, Date dateCreated) {
        this.userId = userId;
        this.typeLogin =typeLogin;
        this.userName = userName;
        this.email = email;
        this.role = role;
        this.sex = sex;
        this.fullName = fullName;
        this.phoneNumber = phoneNumber;
        this.birthDate = birthDate;
        this.avatar = avatar;
        this.city = city;
        this.district = district;
        this.ward = ward;
        this.street = street;
        this.isDeleted = isDeleted;
        this.dateUpdated = dateUpdated;
        this.dateCreated = dateCreated;
    }

}
