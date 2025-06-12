package com.hmdrinks.Repository;

import com.hmdrinks.Entity.Contact;
import com.hmdrinks.Entity.Voucher;
import com.hmdrinks.Enum.Status_Contact;
import com.hmdrinks.Service.ContactService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface ContactRepository extends JpaRepository<Contact, Integer> {

    Contact findByContactId(int contactId);
    Contact findByContactIdAndIsDeletedFalse(int contactId);
    List<Contact> findByIsDeletedFalse();
    Page<Contact> findAllByStatus(Status_Contact status,Pageable pageable);
    List<Contact> findAllByStatus(Status_Contact status);
    Page<Contact> findAll(Pageable pageable);
    @Query("""
    SELECT c.contactId AS contactId, c.description AS description, c.status AS status,
           c.isDeleted AS isDeleted, c.createDate AS createDate, c.updateDate AS updateDate,
           c.dateDeleted AS dateDeleted, c.fullName AS fullName, c.phoneNumber AS phoneNumber,
           c.email AS email
    FROM Contact c
""")
    Page<ContactService.ContactProjection> findAllProjected(Pageable pageable);

    @Query("SELECT COUNT(c) FROM Contact c")
    long countAllContacts();

}
