package company.services;

import company.model.TestRecord;
import company.model.security.User;

import java.util.Set;

public interface TestRecordService {
        TestRecord save(TestRecord object);

        TestRecord createTestRecord(String recordName, String username);

        TestRecord updateTestRecord(Long testRecordID, Long userID, String recordName);

        TestRecord findByRecordName(String recordName);

        TestRecord findById(Long id);

        Set<TestRecord> findAllTestRecordsByUsername(String username);

        Set<TestRecord> findAll();

        void delete(TestRecord objectT);

        void deleteById(Long id);

        void deleteTestRecordAndUpdateUser(Long testRecordID, User associatedUser);
}
