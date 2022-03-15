package com.smart.controller;

import java.util.Random;

import javax.servlet.http.HttpSession;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.smart.dao.UserRepository;
import com.smart.entities.User;
import com.smart.service.EmailService;

@Controller
public class ForgotController {
	
	@Autowired
	private EmailService emailService;
	
	@Autowired
	private UserRepository userRepository;
	
	@Autowired
	private BCryptPasswordEncoder bCrypt;
	
	
	//email id form open handler
	@RequestMapping("/forgot")
	public String openEmailForm()
	{
		return "forgot_email_form";
	}
	
	@PostMapping("/send-otp")
	public String sendOtp(@RequestParam("email") String email, HttpSession session)
	{
		System.out.println("Email "+email);
		
//		Generating four digit of Otp
		
		Random random = new Random();
		int otp = random.nextInt(999999);
		System.out.println(otp);
		
//		code for send otp through email
		
		String subject = "OTP from SCM";
		String message = ""
				+"<div style= 'border:1px solid #e2e2e2; padding:20px'>"
				
				+"OTP is : "
				+"<h1>"
				+"<b>" +otp
				+"</n>"
				+"</h1>"
				+"</div>";
		
		
		String to = email;
		
		
		boolean flag = this.emailService.sendEmail(subject, message, to);
		
		if(flag)
		{
			
			session.setAttribute("myotp",otp);
			session.setAttribute("email", email);
			return "verify_otp";
		}
		else {
			
			session.setAttribute("message","check you email id");

			return "forgot_email_form";
			
		}
		
		
	}
	
	
	
//	verify otp  controller
	
	@PostMapping("/verify-otp")
	public String verifyOtp(@RequestParam("otp") int otp, HttpSession session)
	{
		int myOtp = (int)session.getAttribute("myotp");
		
		String email=(String)session.getAttribute("email");
		
		if(myOtp==otp)
		{
			//
			
			User user = this.userRepository.getUserByUserName(email);
			
			if(user==null)
			{
				//send error message
				session.setAttribute("message","No user found with this email ");

				return "forgot_email_form";
				
				
			}
			else {
				//send change password form
			}
			
			
			return "password_change_form";
		}
		
		else {
			session.setAttribute("message", "you have entered wrong otp");
			return "verify_otp";
		}
		
	}
	
//	change password
	@PostMapping("/change-password")
	public String changePassword(@RequestParam("newpassword") String newpassword,HttpSession session)
	{
		String email=(String)session.getAttribute("email");
		User user = this.userRepository.getUserByUserName(email);
		user.setPassword(this.bCrypt.encode(newpassword));
		this.userRepository.save(user);
	
		return "redirect:/signin?change=password changed successfully..";
	}
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	

}
