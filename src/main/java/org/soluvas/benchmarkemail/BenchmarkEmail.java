package org.soluvas.benchmarkemail;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.TreeNode;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.joda.JodaModule;
import com.google.common.collect.ImmutableMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Created by ceefour on 12/17/15.
 */
public class BenchmarkEmail {
    private static final Logger log = LoggerFactory.getLogger(BenchmarkEmail.class);
    protected static final ObjectMapper MAPPER = new ObjectMapper();

    static {
        MAPPER.registerModule(new JodaModule());
    }

    private String accessToken;
    private InterfaceBMEApi bmeServices;

    public InterfaceBMEApi getBmeServices() {
        return bmeServices;
    }

    public void setBmeServices(InterfaceBMEApi bmeServices) {
        this.bmeServices = bmeServices;
    }

    public String getAccessToken() {
        return accessToken;
    }

    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }

    /**
     * Retrieves the array of lists
     */
    public List<ContactList> listGet(String filter, int pageNumber, int pageSize, String orderBy, String sortOrder) {
        log.info("Getting contact lists filter={} page={}/{} order={} {} ...",
                filter, pageNumber, pageSize, orderBy, sortOrder);
        final List<Map<String, Object>> contactListMaps = bmeServices.listGet(accessToken, filter, pageNumber, pageSize, orderBy, sortOrder);
        final List<ContactList> contactLists = contactListMaps.stream().map(it -> {
            final TreeNode node = MAPPER.valueToTree(it);
            try {
                return MAPPER.treeToValue(node, ContactList.class);
            } catch (JsonProcessingException e) {
                throw new BenchmarkEmailException(e, "Cannot parse: %s", it);
            }
        }).collect(Collectors.toList());
        log.info("Got {} contact lists: {}", contactLists.size(),
                contactLists.stream().map(ContactList::getName).toArray());
        return contactLists;
    }

    /**
     * Adding contacts to an existing list
     */
    public int listAddContacts(long listId, List<Contact> contacts) {
        log.info("Adding {} contacts to {} ... {}",
                contacts.size(), listId, contacts.stream().map(Contact::getEmail).toArray());
        final List<Map<String, Object>> contactMaps = contacts.stream().map(it -> {
            final JsonNode tree = MAPPER.valueToTree(it);
            try {
                final Map<String, Object> contactMap = MAPPER.treeToValue(tree, Map.class);
                // Email -> email, First Name -> firstname, Last Name -> lastname
                contactMap.put("firstname", contactMap.remove("First Name"));
                contactMap.put("lastname", contactMap.remove("Last Name"));
                return contactMap;
            } catch (JsonProcessingException e) {
                throw new BenchmarkEmailException(e, "Cannot convert %s to Map", it);
            }
        }).collect(Collectors.toList());
        final int result = bmeServices.listAddContacts(accessToken, String.valueOf(listId), contactMaps);
        // 1: OK
        // -2: Already exists
        final ImmutableMap<Integer, String> codes = ImmutableMap.of(1, "Added", -2, "Contact(s) already exist");
        log.info("Added {} contacts to {}: {} ({})",
                contacts.size(), listId, result, codes.get(result));
        return result;
    }

    public List<Contact> listGetContactsAllFields(long listId, String filter, int pageNumber, int pageSize, String orderBy, String sortOrder) {
        log.info("Get {}'s contacts all fields filter={} page={}/{} order={},{} ...",
                listId, filter, pageNumber, pageSize, orderBy, sortOrder);
        final List<Map<String, Object>> rawResults = bmeServices.listGetContactsAllFields(accessToken, Long.toString(listId), filter,
                pageNumber, pageSize, orderBy, sortOrder);
        log.info("Got {} {}'s contacts all fields filter={} page={}/{} order={},{}: {}",
                rawResults.size(), listId, filter, pageNumber, pageSize, orderBy, sortOrder, rawResults);
        final List<Contact> contacts = rawResults.stream().map(it -> {
            final JsonNode jsonNode = MAPPER.valueToTree(it);
            try {
                final Contact contact = MAPPER.treeToValue(jsonNode, Contact.class);
                return contact;
            } catch (JsonProcessingException e) {
                throw new BenchmarkEmailException(e, "Cannot convert %s to Contact", it);
            }
        }).collect(Collectors.toList());
        return contacts;
    }

}
