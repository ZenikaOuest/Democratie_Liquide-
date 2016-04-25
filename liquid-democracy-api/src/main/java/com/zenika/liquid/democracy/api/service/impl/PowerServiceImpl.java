package com.zenika.liquid.democracy.api.service.impl;

import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;

import com.zenika.liquid.democracy.api.exception.commons.CloseSubjectException;
import com.zenika.liquid.democracy.api.exception.power.AddPowerOnNonExistingCategoryException;
import com.zenika.liquid.democracy.api.exception.power.AddPowerOnNonExistingSubjectException;
import com.zenika.liquid.democracy.api.exception.power.DeleteNonExistingPowerException;
import com.zenika.liquid.democracy.api.exception.power.DeletePowerOnNonExistingCategoryException;
import com.zenika.liquid.democracy.api.exception.power.DeletePowerOnNonExistingSubjectException;
import com.zenika.liquid.democracy.api.exception.power.PowerIsNotCorrectException;
import com.zenika.liquid.democracy.api.exception.power.UserAlreadyGavePowerException;
import com.zenika.liquid.democracy.api.exception.power.UserGivePowerToHimselfException;
import com.zenika.liquid.democracy.api.exception.vote.UserAlreadyVoteException;
import com.zenika.liquid.democracy.api.persistence.CategoryRepository;
import com.zenika.liquid.democracy.api.persistence.SubjectRepository;
import com.zenika.liquid.democracy.api.service.PowerService;
import com.zenika.liquid.democracy.api.util.PowerUtil;
import com.zenika.liquid.democracy.authentication.service.CollaboratorService;
import com.zenika.liquid.democracy.model.Category;
import com.zenika.liquid.democracy.model.Power;
import com.zenika.liquid.democracy.model.Subject;

@Service
@EnableRetry
@Retryable(OptimisticLockingFailureException.class)
public class PowerServiceImpl implements PowerService {

	@Autowired
	private SubjectRepository subjectRepository;

	@Autowired
	private CategoryRepository categoryRepository;

	@Autowired
	private CollaboratorService collaboratorService;

	public void addPowerOnSubject(String subjectUuid, Power power)
	        throws AddPowerOnNonExistingSubjectException, UserAlreadyGavePowerException,
	        UserGivePowerToHimselfException, CloseSubjectException, UserAlreadyVoteException {

		String userId = collaboratorService.currentUser().getEmail();

		Optional<Subject> s = subjectRepository.findSubjectByUuid(subjectUuid);
		if (!s.isPresent()) {
			throw new AddPowerOnNonExistingSubjectException();
		}

		boolean addVote = PowerUtil.checkPowerForAddition(power, s.get(), userId);

		PowerUtil.preparePower(power, s.get(), userId, addVote);

		subjectRepository.save(s.get());

	}

	@Override
	public void addPowerOnCategory(String categoryUuid, Power power) throws AddPowerOnNonExistingCategoryException,
	        UserAlreadyGavePowerException, UserGivePowerToHimselfException {
		String userId = collaboratorService.currentUser().getEmail();

		Optional<Category> c = categoryRepository.findCategoryByUuid(categoryUuid);
		if (!c.isPresent()) {
			throw new AddPowerOnNonExistingCategoryException();
		}

		PowerUtil.checkCategoryPowerForAddition(power, c.get(), userId);
		PowerUtil.prepareCategoryPower(power, c.get(), userId);

		List<Subject> subjectsInCategory = subjectRepository.findSubjectByCategoryUuid(categoryUuid);
		for (Subject subject : subjectsInCategory) {
			try {
				Power powerTmp = new Power();
				powerTmp.setCollaboratorIdTo(power.getCollaboratorIdTo());
				addPowerOnSubject(subject.getUuid(), powerTmp);
			} catch (PowerIsNotCorrectException | UserAlreadyVoteException | CloseSubjectException e) {
				// if this appends, we just don't propagate power
				System.out.println("ERROR DELEGATION");
			}
		}

		categoryRepository.save(c.get());
	}

	public void deletePowerOnSubject(String subjectUuid) throws DeletePowerOnNonExistingSubjectException,
	        DeleteNonExistingPowerException, CloseSubjectException, UserAlreadyVoteException {

		String userId = collaboratorService.currentUser().getEmail();

		Optional<Subject> s = subjectRepository.findSubjectByUuid(subjectUuid);
		if (!s.isPresent()) {
			throw new DeletePowerOnNonExistingSubjectException();
		}

		Power power = PowerUtil.checkPowerForDelete(s.get(), userId);

		s.get().removePower(power);

		subjectRepository.save(s.get());

	}

	@Override
	public void deletePowerOnCategory(String categoryUuid)
	        throws DeletePowerOnNonExistingCategoryException, DeleteNonExistingPowerException {
		String userId = collaboratorService.currentUser().getEmail();

		Optional<Category> c = categoryRepository.findCategoryByUuid(categoryUuid);
		if (!c.isPresent()) {
			throw new DeletePowerOnNonExistingCategoryException();
		}

		Power power = PowerUtil.checkCategoryPowerForDelete(c.get(), userId);

		c.get().removePower(power);

		List<Subject> subjectsInCategory = subjectRepository.findSubjectByCategoryUuid(categoryUuid);
		for (Subject subject : subjectsInCategory) {
			try {
				Optional<Power> powerTmp = subject.findPower(userId);
				if (powerTmp.isPresent() && powerTmp.get().getCollaboratorIdTo().equals(power.getCollaboratorIdTo())) {
					deletePowerOnSubject(subject.getUuid());
				}
			} catch (DeletePowerOnNonExistingSubjectException | DeleteNonExistingPowerException | CloseSubjectException
			        | UserAlreadyVoteException e) {
				// if this appends, we just don't propagate power
				System.out.println("ERROR DELEGATION");
			}
		}

		categoryRepository.save(c.get());

	}
}
