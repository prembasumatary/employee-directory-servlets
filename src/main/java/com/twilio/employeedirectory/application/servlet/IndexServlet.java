package com.twilio.employeedirectory.application.servlet;


import com.google.inject.persist.Transactional;
import com.twilio.employeedirectory.domain.error.EmployeeLoadException;
import com.twilio.employeedirectory.domain.model.Employee;
import com.twilio.employeedirectory.domain.repository.EmployeeRepository;
import com.twilio.employeedirectory.domain.service.EmployeeDirectoryService;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.type.CollectionType;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.util.List;
import java.util.Optional;
import java.util.logging.Logger;

@Singleton
public class IndexServlet extends HttpServlet {

  private static final Logger LOG = Logger.getLogger(IndexServlet.class.getName());

  private static final String JSON_PATH = "seed-data.json";

  private EmployeeRepository repository;

  private EmployeeDirectoryService employeeService;

  @Inject
  public IndexServlet(EmployeeRepository repository, EmployeeDirectoryService employeeService) {
    this.repository = repository;
    this.employeeService = employeeService;
  }

  @Override
  public void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException,
      IOException {
    req.setAttribute("firstEmployee", repository.findFirstEmployee());
    req.setAttribute("employeeMatch", Optional.empty());
    req.setAttribute("query", "");
    req.getRequestDispatcher("index.jsp").forward(req, resp);
  }


  @Override
  protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException,
      IOException {
    Optional<String> fullNameQuery = Optional.of(req.getParameter("query"));
    req.setAttribute("firstEmployee", Optional.empty());
    if (fullNameQuery.isPresent()) {
      req.setAttribute("employeeMatch",
          Optional.of(employeeService.queryEmployee(fullNameQuery.get())));
    } else {
      req.setAttribute("employeeMatch", fullNameQuery);
    }
    req.setAttribute("query", fullNameQuery.orElse(""));
    req.getRequestDispatcher("index.jsp").forward(req, resp);
  }

  @Override
  @Transactional
  public void init(ServletConfig config) throws ServletException {
    super.init(config);
    LOG.info("Seeding Marvel Heroes...");
    try {
      File employeeJsonFile = new File(getResourceURI());

      ObjectMapper objectMapper = new ObjectMapper();
      CollectionType collectionType =
          objectMapper.getTypeFactory().constructCollectionType(List.class, Employee.class);
      List<Employee> employees = objectMapper.readValue(employeeJsonFile, collectionType);
      repository.addAll(employees);
    } catch (Exception e) {
      throw new EmployeeLoadException(e);
    }
  }

  private URI getResourceURI() {
    Optional<URL> url =
        Optional.ofNullable(this.getClass().getResource(File.separator + JSON_PATH));
    return url.map((URL u) -> {
      try {
        return u.toURI();
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
    }).orElseThrow(
        () -> new EmployeeLoadException(String.format("Not possible to retrieve resource: %s",
            JSON_PATH)));
  }
}
