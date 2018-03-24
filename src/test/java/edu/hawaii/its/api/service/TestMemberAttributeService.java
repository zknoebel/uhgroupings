package edu.hawaii.its.api.service;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import edu.hawaii.its.api.type.GroupingsServiceResult;
import edu.hawaii.its.api.type.GroupingsServiceResultException;
import edu.hawaii.its.groupings.configuration.SpringBootWebApplication;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.util.Assert;

import javax.annotation.PostConstruct;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

@ActiveProfiles("integrationTest")
@RunWith(SpringRunner.class)
@SpringBootTest(classes = { SpringBootWebApplication.class })
public class TestMemberAttributeService {

    @Value("${groupings.api.test.grouping_many}")
    private String GROUPING;
    @Value("${groupings.api.test.grouping_many_basis}")
    private String GROUPING_BASIS;
    @Value("${groupings.api.test.grouping_many_include}")
    private String GROUPING_INCLUDE;
    @Value("${groupings.api.test.grouping_many_exclude}")
    private String GROUPING_EXCLUDE;
    @Value("${groupings.api.test.grouping_many_owners}")
    private String GROUPING_OWNERS;

    @Value("${groupings.api.success}")
    private String SUCCESS;

    @Value("${groupings.api.test.usernames}")
    private String[] username;

    @Value("${groupings.api.failure}")
    private String FAILURE;

    @Autowired
    GroupAttributeService groupAttributeService;

    @Autowired
    GroupingAssignmentService groupingAssignmentService;

    @Autowired
    private MemberAttributeService memberAttributeService;

    @Autowired
    private MembershipService membershipService;

    @Autowired
    public Environment env; // Just for the settings check.

    @PostConstruct
    public void init() {
        Assert.hasLength(env.getProperty("grouperClient.webService.url"),
                "property 'grouperClient.webService.url' is required");
        Assert.hasLength(env.getProperty("grouperClient.webService.login"),
                "property 'grouperClient.webService.login' is required");
        Assert.hasLength(env.getProperty("grouperClient.webService.password"),
                "property 'grouperClient.webService.password' is required");
    }

    @Before
    public void setUp() {
        groupAttributeService.changeListservStatus(GROUPING, username[0], true);
        groupAttributeService.changeOptInStatus(GROUPING, username[0], true);
        groupAttributeService.changeOptOutStatus(GROUPING, username[0], true);

        //put in include
        membershipService.addGroupingMemberByUsername(username[0], GROUPING, username[0]);
        membershipService.addGroupingMemberByUsername(username[0], GROUPING, username[1]);
        membershipService.addGroupingMemberByUsername(username[0], GROUPING, username[2]);

        //remove from exclude
        membershipService.addGroupingMemberByUsername(username[0], GROUPING, username[4]);
        membershipService.addGroupingMemberByUsername(username[0], GROUPING, username[5]);

        //add to exclude
        membershipService.deleteGroupingMemberByUsername(username[0], GROUPING, username[3]);
    }

    @Test
    public void isOwnerTest() {
        assertTrue(memberAttributeService.isOwner(GROUPING, username[0]));
    }

    @Test
    public void assignRemoveOwnershipTest() {
        //expect to fail
        GroupingsServiceResult assignOwnershipFail;
        GroupingsServiceResult removeOwnershipFail;

        assertTrue(memberAttributeService.isOwner(GROUPING, username[0]));
        assertFalse(memberAttributeService.isOwner(GROUPING, username[1]));
        assertFalse(memberAttributeService.isOwner(GROUPING, username[2]));

        try {
            assignOwnershipFail = memberAttributeService.assignOwnership(GROUPING, username[1], username[1]);
        } catch (GroupingsServiceResultException gsre) {
            assignOwnershipFail = gsre.getGsr();
        }
        assertFalse(memberAttributeService.isOwner(GROUPING, username[1]));
        assertTrue(assignOwnershipFail.getResultCode().startsWith(FAILURE));

        GroupingsServiceResult assignOwnershipSuccess =
                memberAttributeService.assignOwnership(GROUPING, username[0], username[1]);
        assertTrue(memberAttributeService.isOwner(GROUPING, username[1]));
        assertTrue(assignOwnershipSuccess.getResultCode().startsWith(SUCCESS));

        try {
            removeOwnershipFail = memberAttributeService.removeOwnership(GROUPING, username[2], username[1]);
        } catch (GroupingsServiceResultException gsre) {
            removeOwnershipFail = gsre.getGsr();
        }

        assertTrue(memberAttributeService.isOwner(GROUPING, username[1]));
        assertTrue(removeOwnershipFail.getResultCode().startsWith(FAILURE));

        GroupingsServiceResult removeOwnershipSuccess =
                memberAttributeService.removeOwnership(GROUPING, username[0], username[1]);
        assertFalse(memberAttributeService.isOwner(GROUPING, username[1]));
        assertTrue(removeOwnershipSuccess.getResultCode().startsWith(SUCCESS));

        //have an owner remove itself
        assignOwnershipSuccess = memberAttributeService.assignOwnership(GROUPING, username[0], username[1]);
        assertTrue(memberAttributeService.isOwner(GROUPING, username[1]));
        removeOwnershipSuccess = memberAttributeService.removeOwnership(GROUPING, username[1], username[1]);
        assertFalse(memberAttributeService.isOwner(GROUPING, username[1]));

    }

    @Test
    public void inGroupTest() {
        assertTrue(memberAttributeService.isMember(GROUPING_INCLUDE, username[1]));
        assertFalse(memberAttributeService.isMember(GROUPING_INCLUDE, username[3]));

        assertTrue(memberAttributeService.isMember(GROUPING_EXCLUDE, username[3]));
        assertFalse(memberAttributeService.isMember(GROUPING_EXCLUDE, username[1]));
    }

}