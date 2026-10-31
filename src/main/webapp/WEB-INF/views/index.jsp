<%@ page contentType="text/html;charset=UTF-8" trimDirectiveWhitespaces="true" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:set var="contextPath" value="${pageContext.request.contextPath}" />
<!doctype html>
<html lang="en">

<head>
  <meta charset="utf-8">
  <meta name="viewport" content="width=device-width, initial-scale=1">
  <link rel="shortcut icon" type="image/x-icon" href="${contextPath}/static/favicon.ico">
  <link rel="stylesheet" href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.8/dist/css/bootstrap.min.css"
    integrity="sha384-sRIl4kxILFvY47J16cr9ZwB07vP4J8+LH7qKQnuqkuIAvNWLzeN8tE5YBujZqJLB" crossorigin="anonymous">
  <link rel="stylesheet" href="https://use.fontawesome.com/releases/v7.3.1/css/all.css"
    integrity="sha384-qrALq7+6jBOZIQsNnT6xGkMDru64qD6uTlDra39xrt2SoXl4pO3FX6Roz/RpR/BS" crossorigin="anonymous">
  <link rel="stylesheet" href="${contextPath}/static/css/main.css">
  <title>Hello, world!</title>
</head>

<body>

  <main>
    <%-- Bootstrap 5 dropped .jumbotron; the hero uses utilities instead. --%>
    <section class="px-4 py-5 mb-4 bg-body-secondary rounded-3 text-center">
      <div class="container">
        <h1><img class="img-fluid" src="https://nalbam.com/static/logo/spring-tomcat.png" alt="Apache Tomcat"></h1>
        <h3 class="hello-title">${host}</h3>
        <p>${date}</p>
        <p>${cluster}</p>
        <p>${profile}</p>
        <p>${message}</p>
        <p>${version}</p>
        <p>
          <button type="button" class="btn btn-primary btn-thumbs-up"><i class="fa-regular fa-thumbs-up"
              aria-hidden="true"></i><span class="visually-hidden">Thumbs up</span> <span
              id="thumbs-up-count">0</span></button>
          <button type="button" class="btn btn-danger btn-thumbs-down"><i class="fa-regular fa-thumbs-down"
              aria-hidden="true"></i><span class="visually-hidden">Thumbs down</span> <span
              id="thumbs-down-count">0</span></button>
        </p>
      </div>
    </section>
  </main>

  <script src="https://code.jquery.com/jquery-4.0.0.min.js"
    integrity="sha384-fgGyf7Mo7DURSOMnOy7ed+dkq5Job205Gnzu6QIg0BOHKaqt4D76Dt8VlDCzcMHV" crossorigin="anonymous"></script>
  <script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.8/dist/js/bootstrap.min.js"
    integrity="sha384-G/EV+4j2dNv+tEPo3++6LCgdCROaejBqfUeNjuKAiuXbjrxilcCdDz6ZAVfHWe1Y" crossorigin="anonymous"></script>
  <%-- External counter: returns 404 and its backend API was removed. Known failure, see README. --%>
  <script src="https://nalbam.com/static/sample/counter.js"></script>
  <script src="${contextPath}/static/js/main.js"></script>
</body>

</html>
