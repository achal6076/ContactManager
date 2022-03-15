package com.smart.controller;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.Principal;
import java.util.List;
import java.util.Optional;

import javax.servlet.http.HttpSession;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import com.smart.dao.ContactRepository;
import com.smart.dao.UserRepository;
import com.smart.entities.Contact;
import com.smart.entities.User;
import com.smart.helper.Message;

@Controller
@RequestMapping("/user")	
public class UserController {
	@Autowired
	private BCryptPasswordEncoder bCryptPasswordEncoder;

	@Autowired
	private UserRepository userRepository;
	@Autowired
	private ContactRepository contactRepository;

//	method for adding data to response
	@ModelAttribute
	public void addCommonData(Model model, Principal principal) {

		String userName = principal.getName();
		System.out.println(userName);

		User user = userRepository.getUserByUserName(userName);
		System.out.println("USER" + user);

		model.addAttribute("user", user);
	}

//	Dashboard home

	@RequestMapping("/index")
	public String dashboard(Model model, Principal principal) {
		String userName = principal.getName();
		System.out.println(userName);

		User user = userRepository.getUserByUserName(userName);
		System.out.println("USER" + user);

		model.addAttribute("user", user);
		return "normal/user_dashboard";

	}

//	open and add form controller

	@GetMapping("add-contact")
	public String openAddContactForm(Model model) {
		model.addAttribute("title", "Add Contact");
		model.addAttribute("contact", new Contact());
		return "normal/add_contact_form";
	}

	@PostMapping("process-contact")
	public String processContact(@ModelAttribute Contact contact, @RequestParam("profileImage") MultipartFile file,
			Principal principal, HttpSession session) {

		try {

			String name = principal.getName();
			User user = this.userRepository.getUserByUserName(name);
//		
//		if(3>2)
//		{
//		throw new Exception();
//		}
//		

//		processing and uploading file

			if (file.isEmpty()) {

				System.out.println("empty image");
				contact.setImage("contact.png");

			} else {

				contact.setImage(file.getOriginalFilename());
				File saveFile = new ClassPathResource("static/img").getFile();

				Path path = Paths.get(saveFile.getAbsolutePath() + File.separator + file.getOriginalFilename());

				Files.copy(file.getInputStream(), path, StandardCopyOption.REPLACE_EXISTING);

				System.out.println("image uplaoded successfully");

			}

			user.getContacts().add(contact);
			contact.setUser(user);
			this.userRepository.save(user);

			System.out.println("data" + contact);
			System.out.println("Added to data base");

			// message successful

			session.setAttribute("message", new Message("Your contact added successfully ! add more...", "success"));

		} catch (Exception e) {
			System.out.println("ERROR" + e.getMessage());
			e.printStackTrace();

			// Error message
			session.setAttribute("message", new Message("something went wrong try again...", "danger"));

		}
		return "normal/add_contact_form";
	}

//	show contact handler
	// per page = 5[n]
	// current page = 0[page]

	@GetMapping("/show-contacts/{page}")
	public String showContacts(@PathVariable("page") Integer page, Model m, Principal principal) {
		m.addAttribute("title", "show user Contacts");
		String userName = principal.getName();
		User user = this.userRepository.getUserByUserName(userName);

		Pageable pageable = PageRequest.of(page, 5);
		Page<Contact> contacts = this.contactRepository.findContactByUser(user.getId(), pageable);
		m.addAttribute("contacts", contacts);
		m.addAttribute("currentPage", page);
		m.addAttribute("totalPages", contacts.getTotalPages());

		return "normal/show_contacts";
	}

	// Showing particular contact details
	@RequestMapping("/{cId}/contact")
	public String showContactDetail(@PathVariable("cId") Integer cId, Model model, Principal principal) {
		System.out.println("cId" + cId);
		Optional<Contact> contactoptional = this.contactRepository.findById(cId);
		Contact contact = contactoptional.get();

		String userName = principal.getName();
		User user = this.userRepository.getUserByUserName(userName);
		if (user.getId() == contact.getUser().getId()) {

			model.addAttribute("contact", contact);
			model.addAttribute("title", contact.getName());
		}

		return "normal/contact_detail";
	}

	// Delete contact handler
	@GetMapping("/delete/{cid}")
	public String deleteContact(@PathVariable("cid") Integer cId, Model model, HttpSession session,
			Principal principal) {
		Optional<Contact> contactOptional = this.contactRepository.findById(cId);
		Contact contact = contactOptional.get();

		User user = this.userRepository.getUserByUserName(principal.getName());
		user.getContacts().remove(contact);

		this.userRepository.save(user);

		this.contactRepository.delete(contact);

		session.setAttribute("message", new Message("Contact deleted successfully", "success"));

		return "redirect:/user/show-contacts/0";
	}

	// Open update form handler
	@PostMapping("/update-contact/{cid}")
	public String updateForm(@PathVariable("cid") Integer cid, Model m) {

		m.addAttribute("title", "Update Contact");
		Contact contact = this.contactRepository.findById(cid).get();
		m.addAttribute("contact", contact);
		return "normal/update_form";
	}

	// Update contact handler
	@RequestMapping(value = "process-update", method = RequestMethod.POST)
	public String updateHandler(@ModelAttribute Contact contact, @RequestParam("profileImage") MultipartFile file,
			Model m, HttpSession session, Principal principal) {

		try {

//			old contact details
			Contact oldcontactDetail = this.contactRepository.findById(contact.getcId()).get();

			if (!file.isEmpty()) {

//				Delete old file

				File deleteFile = new ClassPathResource("static/img").getFile();
				File file1 = new File(deleteFile, oldcontactDetail.getImage());
				file1.delete();

//				Update new file

				File saveFile = new ClassPathResource("static/img").getFile();

				Path path = Paths.get(saveFile.getAbsolutePath() + File.separator + file.getOriginalFilename());

				Files.copy(file.getInputStream(), path, StandardCopyOption.REPLACE_EXISTING);

				contact.setImage(file.getOriginalFilename());

			}

			else {
				contact.setImage(oldcontactDetail.getImage());
			}

			User user = this.userRepository.getUserByUserName(principal.getName());
			contact.setUser(user);

			this.contactRepository.save(contact);

			session.setAttribute("message", new Message("Your contact is updated", "success"));

		} catch (Exception e) {
			// TODO: handle exception
		}

		System.out.println("Contact name " + contact.getName());
		System.out.println("Contact Id " + contact.getcId());

		return "redirect:/user/" + contact.getcId() + "/contact";
	}

//	Your profile Handler

	@RequestMapping("/profile")
	public String yourProfile(Model model) {
		model.addAttribute("title", "Profile page");
		return "normal/profile";
	}

	// settings handler
	@GetMapping("/settings")
	public String openSettings() {

		return "normal/settings";
	}

	// Change password handler
	@PostMapping("/change-password")
	public String changePassword(@RequestParam("oldPassword") String oldPassword,
			@RequestParam("newPassword") String newPassword, Principal principal,HttpSession session) {

		System.out.println("Old password " + oldPassword);
		System.out.println("New password " + newPassword);

		String userName = principal.getName();
		User currentUser = this.userRepository.getUserByUserName(userName);
		System.out.println(currentUser.getPassword());
		
		if(this.bCryptPasswordEncoder.matches(oldPassword, currentUser.getPassword()))
		{
			currentUser.setPassword(this.bCryptPasswordEncoder.encode(newPassword));
			this.userRepository.save(currentUser);
			session.setAttribute("message", new Message("Your passsword have changed successfully","success"));
		}
		else
		{
			session.setAttribute("message", new Message("Please Enter your correct  password","danger"));
			return "redirect:/user/settings";
		
		}
		
			
		
		return "redirect:/user/index";
	}

}
