package com.zenika.liquid.democracy.api.controller;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.Optional;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.boot.test.WebIntegrationTest;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.web.client.DefaultResponseErrorHandler;
import org.springframework.web.client.RestTemplate;

import com.zenika.liquid.democracy.Application;
import com.zenika.liquid.democracy.api.category.persistence.CategoryRepository;
import com.zenika.liquid.democracy.api.subject.persistence.SubjectRepository;
import com.zenika.liquid.democracy.model.Category;
import com.zenika.liquid.democracy.model.Power;
import com.zenika.liquid.democracy.model.Proposition;
import com.zenika.liquid.democracy.model.Subject;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes = Application.class)
@WebIntegrationTest(randomPort = true)
@ActiveProfiles("test")
public class PowerControllerTest {

	@Autowired
	private SubjectRepository subjectRepository;

	@Autowired
	private CategoryRepository categoryRepository;

	@Value("${local.server.port}")
	private int serverPort;

	private RestTemplate template;

	@Before
	public void setUp() throws Exception {
		template = new RestTemplate();
		template.setErrorHandler(new DefaultResponseErrorHandler() {
			protected boolean hasError(HttpStatus statusCode) {
				return statusCode.series() == HttpStatus.Series.SERVER_ERROR;
			}
		});
		subjectRepository.deleteAll();
	}

	@Test
	public void addPowerOnSubjectTest() {
		Subject s = new Subject();
		s.setTitle("Title");
		s.setDescription("Description");
		Proposition p1 = new Proposition();
		Proposition p2 = new Proposition();
		s.getPropositions().add(p1);
		s.getPropositions().add(p2);
		p1.setTitle("P1 title");
		p2.setTitle("P2 title");
		subjectRepository.save(s);

		Power p = new Power();
		p.setCollaboratorIdTo("julie.bourhis@zenika.com");

		ResponseEntity<Object> addResp = template.exchange(
		        "http://localhost:" + serverPort + "api/powers/subjects/" + s.getUuid(), HttpMethod.PUT,
		        new HttpEntity<>(p), Object.class);

		assertNotNull(addResp);
		assertEquals(HttpStatus.OK.value(), addResp.getStatusCode().value());

		Optional<Subject> savedSubject = subjectRepository.findSubjectByUuid(s.getUuid());
		assertEquals(true, savedSubject.isPresent());
		assertEquals(true, savedSubject.get().findPower("sandra.parlant@zenika.com").isPresent());
	}

	@Test
	public void addPowerOnSubjectAgainTest() {
		Subject s = new Subject();
		s.setTitle("Title");
		s.setDescription("Description");
		Proposition p1 = new Proposition();
		Proposition p2 = new Proposition();
		s.getPropositions().add(p1);
		s.getPropositions().add(p2);
		p1.setTitle("P1 title");
		p2.setTitle("P2 title");

		Power p = new Power();
		p.setCollaboratorIdFrom("sandra.parlant@zenika.com");
		p.setCollaboratorIdTo("julie.bourhis@zenika.com");

		s.getPowers().add(p);
		subjectRepository.save(s);

		ResponseEntity<Object> addResp = template.exchange(
		        "http://localhost:" + serverPort + "api/powers/subjects/" + s.getUuid(), HttpMethod.PUT,
		        new HttpEntity<>(p), Object.class);

		assertNotNull(addResp);
		assertEquals(HttpStatus.BAD_REQUEST.value(), addResp.getStatusCode().value());
		assertEquals(true, addResp.getBody().toString().contains("UserAlreadyGavePowerException"));
	}

	@Test
	public void addPowerOnNonExistingSubjectTest() {
		Subject s = new Subject();
		s.setTitle("Title");
		s.setDescription("Description");
		Proposition p1 = new Proposition();
		Proposition p2 = new Proposition();
		s.getPropositions().add(p1);
		s.getPropositions().add(p2);
		p1.setTitle("P1 title");
		p2.setTitle("P2 title");
		subjectRepository.save(s);

		Power p = new Power();
		p.setCollaboratorIdTo("julie.bourhis@zenika.com");

		ResponseEntity<Object> addResp = template.exchange(
		        "http://localhost:" + serverPort + "api/powers/subjects/" + s.getUuid() + 1, HttpMethod.PUT,
		        new HttpEntity<>(p), Object.class);

		assertNotNull(addResp);
		assertEquals(HttpStatus.BAD_REQUEST.value(), addResp.getStatusCode().value());
		assertEquals(true, addResp.getBody().toString().contains("AddPowerOnNonExistingSubjectException"));
	}

	@Test
	public void addPowerOnSubjectToHimselfTest() {
		Subject s = new Subject();
		s.setTitle("Title");
		s.setDescription("Description");
		Proposition p1 = new Proposition();
		Proposition p2 = new Proposition();
		s.getPropositions().add(p1);
		s.getPropositions().add(p2);
		p1.setTitle("P1 title");
		p2.setTitle("P2 title");
		subjectRepository.save(s);

		Power p = new Power();
		p.setCollaboratorIdTo("sandra.parlant@zenika.com");

		ResponseEntity<Object> addResp = template.exchange(
		        "http://localhost:" + serverPort + "api/powers/subjects/" + s.getUuid(), HttpMethod.PUT,
		        new HttpEntity<>(p), Object.class);

		assertNotNull(addResp);
		assertEquals(HttpStatus.BAD_REQUEST.value(), addResp.getStatusCode().value());
		assertEquals(true, addResp.getBody().toString().contains("CircularPowerDependencyException"));
	}

	@Test
	public void addPowerOnCategoryTest() {
		Category c = new Category();
		c.setTitle("c1");
		categoryRepository.save(c);

		Subject s = new Subject();
		s.setTitle("Title");
		s.setDescription("Description");
		Proposition p1 = new Proposition();
		Proposition p2 = new Proposition();
		s.getPropositions().add(p1);
		s.getPropositions().add(p2);
		p1.setTitle("P1 title");
		p2.setTitle("P2 title");
		s.setCategory(c);
		subjectRepository.save(s);

		Power p = new Power();
		p.setCollaboratorIdTo("julie.bourhis@zenika.com");

		ResponseEntity<Object> addResp = template.exchange(
		        "http://localhost:" + serverPort + "api/powers/categories/" + c.getUuid(), HttpMethod.PUT,
		        new HttpEntity<>(p), Object.class);

		assertNotNull(addResp);
		assertEquals(HttpStatus.OK.value(), addResp.getStatusCode().value());

		Optional<Subject> savedSubject = subjectRepository.findSubjectByUuid(s.getUuid());
		assertEquals(true, savedSubject.isPresent());
		assertEquals(true, savedSubject.get().findPower("sandra.parlant@zenika.com").isPresent());

		Optional<Category> savedCategory = categoryRepository.findCategoryByUuid(c.getUuid());
		assertEquals(true, savedCategory.isPresent());
		assertEquals(true, savedCategory.get().findPower("sandra.parlant@zenika.com").isPresent());
	}

	@Test
	public void deletePowerOnSubjectTest() {
		Subject s = new Subject();
		s.setTitle("Title");
		s.setDescription("Description");
		Proposition p1 = new Proposition();
		Proposition p2 = new Proposition();
		s.getPropositions().add(p1);
		s.getPropositions().add(p2);
		p1.setTitle("P1 title");
		p2.setTitle("P2 title");

		Power p = new Power();
		p.setCollaboratorIdFrom("sandra.parlant@zenika.com");
		p.setCollaboratorIdTo("julie.bourhis@zenika.com");

		s.getPowers().add(p);
		subjectRepository.save(s);

		ResponseEntity<Object> addResp = template.exchange(
		        "http://localhost:" + serverPort + "api/powers/subjects/" + s.getUuid(), HttpMethod.DELETE,
		        new HttpEntity<>(null), Object.class);

		assertNotNull(addResp);
		assertEquals(HttpStatus.OK.value(), addResp.getStatusCode().value());
	}

	@Test
	public void deletePowerOnCategoryTest() {
		Category c = new Category();
		c.setTitle("c1");
		categoryRepository.save(c);

		Subject s = new Subject();
		s.setTitle("Title");
		s.setDescription("Description");
		Proposition p1 = new Proposition();
		Proposition p2 = new Proposition();
		s.getPropositions().add(p1);
		s.getPropositions().add(p2);
		p1.setTitle("P1 title");
		p2.setTitle("P2 title");
		s.setCategory(c);

		Power p = new Power();
		p.setCollaboratorIdFrom("sandra.parlant@zenika.com");
		p.setCollaboratorIdTo("julie.bourhis@zenika.com");

		c.getPowers().add(p);
		categoryRepository.save(c);

		s.getPowers().add(p);
		subjectRepository.save(s);

		ResponseEntity<Object> addResp = template.exchange(
		        "http://localhost:" + serverPort + "api/powers/categories/" + c.getUuid(), HttpMethod.DELETE,
		        new HttpEntity<>(null), Object.class);

		assertNotNull(addResp);
		assertEquals(HttpStatus.OK.value(), addResp.getStatusCode().value());

		Optional<Category> savedCategory = categoryRepository.findCategoryByUuid(c.getUuid());
		assertEquals(savedCategory.get().getPowers().size(), 0);

		Optional<Subject> savedSubject = subjectRepository.findSubjectByUuid(s.getUuid());
		assertEquals(savedSubject.get().getPowers().size(), 0);
	}

	@Test
	public void deletePowerOnCategoryWithinPowerOnSubjectTest() {
		Category c = new Category();
		c.setTitle("c1");
		categoryRepository.save(c);

		Subject s = new Subject();
		s.setTitle("Title");
		s.setDescription("Description");
		Proposition p1 = new Proposition();
		Proposition p2 = new Proposition();
		s.getPropositions().add(p1);
		s.getPropositions().add(p2);
		p1.setTitle("P1 title");
		p2.setTitle("P2 title");
		s.setCategory(c);

		Power p = new Power();
		p.setCollaboratorIdFrom("sandra.parlant@zenika.com");
		p.setCollaboratorIdTo("julie.bourhis@zenika.com");

		c.getPowers().add(p);
		categoryRepository.save(c);

		Power powerS = new Power();
		powerS.setCollaboratorIdFrom("sandra.parlant@zenika.com");
		powerS.setCollaboratorIdTo("guillaume.gerbaud@zenika.com");
		s.getPowers().add(powerS);
		subjectRepository.save(s);

		ResponseEntity<Object> addResp = template.exchange(
		        "http://localhost:" + serverPort + "api/powers/categories/" + c.getUuid(), HttpMethod.DELETE,
		        new HttpEntity<>(null), Object.class);

		assertNotNull(addResp);
		assertEquals(HttpStatus.OK.value(), addResp.getStatusCode().value());

		Optional<Category> savedCategory = categoryRepository.findCategoryByUuid(c.getUuid());
		assertEquals(savedCategory.get().getPowers().size(), 0);

		Optional<Subject> savedSubject = subjectRepository.findSubjectByUuid(s.getUuid());
		assertEquals(savedSubject.get().getPowers().size(), 1);
	}

	@Test
	public void deleteNonExistingPowerOnSubjectTest() {
		Subject s = new Subject();
		s.setTitle("Title");
		s.setDescription("Description");
		Proposition p1 = new Proposition();
		Proposition p2 = new Proposition();
		s.getPropositions().add(p1);
		s.getPropositions().add(p2);
		p1.setTitle("P1 title");
		p2.setTitle("P2 title");

		Power p = new Power();
		p.setCollaboratorIdFrom("sandra.parlantt@zenika.com");
		p.setCollaboratorIdTo("julie.bourhis@zenika.com");

		s.getPowers().add(p);
		subjectRepository.save(s);

		ResponseEntity<Object> addResp = template.exchange(
		        "http://localhost:" + serverPort + "api/powers/subjects/" + s.getUuid(), HttpMethod.DELETE,
		        new HttpEntity<>(null), Object.class);

		assertNotNull(addResp);
		assertEquals(HttpStatus.BAD_REQUEST.value(), addResp.getStatusCode().value());
		assertEquals(true, addResp.getBody().toString().contains("DeleteNonExistingPowerException"));
	}

	@Test
	public void deletePowerOnNonExistingSubjectTest() {
		Subject s = new Subject();
		s.setTitle("Title");
		s.setDescription("Description");
		Proposition p1 = new Proposition();
		Proposition p2 = new Proposition();
		s.getPropositions().add(p1);
		s.getPropositions().add(p2);
		p1.setTitle("P1 title");
		p2.setTitle("P2 title");

		Power p = new Power();
		p.setCollaboratorIdFrom("sandra.parlant@zenika.com");
		p.setCollaboratorIdTo("julie.bourhis@zenika.com");

		s.getPowers().add(p);
		subjectRepository.save(s);

		ResponseEntity<Object> addResp = template.exchange(
		        "http://localhost:" + serverPort + "api/powers/subjects/" + s.getUuid() + 1, HttpMethod.DELETE,
		        new HttpEntity<>(null), Object.class);

		assertNotNull(addResp);
		assertEquals(HttpStatus.BAD_REQUEST.value(), addResp.getStatusCode().value());
		assertEquals(true, addResp.getBody().toString().contains("DeletePowerOnNonExistingSubjectException"));
	}

}
