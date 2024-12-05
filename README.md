# Multi-Agent an AI-driven app focused on personalized learning, where users upload their CV/Resume to receive tailored Udemy course recommendations to advance their developer role

[![Maintenance](https://img.shields.io/badge/Maintained%3F-yes-green.svg)]()
[![Maintainer](https://img.shields.io/static/v1?label=Yevhen%20Ruban&message=Maintainer&color=red)](mailto:yevhen.ruban@extrawest.com)
[![Ask Me Anything !](https://img.shields.io/badge/Ask%20me-anything-1abc9c.svg)]()
[![License](https://img.shields.io/badge/License-Apache_2.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)
![GitHub release](https://img.shields.io/badge/release-v1.0.0-blue)

This application is an AI-driven career assistant designed to analyze CVs or resumes in various formats (PDF, DOC, PNG) and provide users with tailored learning recommendations. By leveraging a multi-agent architecture, the app identifies the user’s professional level, role, and skillset. It then matches this data with a Qdrant-embedded database containing Udemy courses to recommend personalized learning paths that enhance the user’s skills and career progression. Built with Spring Boot 3.3.3, LangChain4j, TogetherAI, and Spring State Machine, the app is a robust and scalable solution for professional development.





https://github.com/user-attachments/assets/1579b9c0-928c-4291-ba5f-f4ecbb0ed318





## Key Features

- **Comprehensive Art Search and Exploration**: Users can search for artworks across various departments and collections from the MET Museum's vast online database. The app provides access to high-quality images, enabling users to explore the details of each piece closely.
- **Multi-Format Resume Parsing**: Analyze CVs in PDF, DOC, and PNG formats, accurately extracting professional details such as roles, skills, and experience levels.
- **AI-Powered Role and Skill Analysis**: Automatically determine the user’s professional level (e.g., Junior, Senior, Architect), developer role (e.g., Backend, Frontend), and technical skillset for precise profiling.
- **Multi-Agent Collaboration**: Employ a multi-agent system with Spring State Machine to enable seamless communication between agents for resume analysis and course recommendations.
- **Integrated Qdrant Database**: Utilize a Qdrant-embedded vector database to store and match user profiles with relevant Udemy courses using semantic search technology.
- **Personalized Course Recommendations**: Recommend tailored Udemy courses that align with the user’s role, level, and skillset, focusing on skill enhancement and career advancement.
- **LangChain4j-Powered Insights**: Leverage LangChain4j for natural language understanding and effective query handling, ensuring accurate and contextually relevant responses.
- **TogetherAI Integration**: Facilitate smooth and intelligent agent interactions using TogetherAI for a cohesive user experience.
- **Spring Boot Scalability**: Built on Spring Boot 3.3.3, ensuring a reliable and scalable backend architecture for seamless performance.
- **Actionable Career Advice**: Provide practical insights and recommendations for improving skills and staying competitive in the professional landscape.
- **Modular and Extensible Design**: Designed for scalability and flexibility, enabling easy integration of new features, databases, or enhancements to meet evolving user needs.

## Tech Stack

- **Java 21**
- **SpringBoot 3.3.3**: Backend framework for building fast and scalable applications.
- **Together AI**: Provides models for describe image. Model: Meta Llama 3.2 90B Vision Instruct Turbo
- **Langchain4j**: Supercharge your Java application with the power of LLMs.

## Running On Local Machine (Linux):

1. Set up the following environment variables.
    - export TOGETHER_AI_API_KEY=your_api_key;
    - export QDRANT_GRPC_HOST=your_qdrant_host;
    - export QDRANT_API_KEY=your_api_key;
2. Run the command: mvn exec:java -Dspring.profiles.active=local
3. Open the following link in your browser: http://localhost:8208/api/swagger-ui/index.html#/

## Contributing

Feel free to open issues or submit pull requests to improve the project. Contributions are welcome!
