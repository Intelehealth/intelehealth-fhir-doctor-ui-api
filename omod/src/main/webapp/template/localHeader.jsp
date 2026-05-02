<spring:htmlEscape defaultHtmlEscape="true" />
<title>Facility</title>
<link rel="stylesheet" href="${pageContext.request.contextPath}/moduleResources/ihmodule/css/style.css">
<link rel="stylesheet" href="${pageContext.request.contextPath}/moduleResources/ihmodule/css/jquery.dataTables.min.css">
<link rel="stylesheet" href="${pageContext.request.contextPath}/moduleResources/ihmodule/css/bootstrap.min.css">

<script src="${pageContext.request.contextPath}/moduleResources/ihmodule/js/jquery-3.6.0.min.js"></script>
<script src="${pageContext.request.contextPath}/moduleResources/ihmodule/js/jquery.dataTables.min.js"></script>

<style>
  /* Module secondary nav: matches ihmodule teal (.btnSubmit / .note), fixes conflict of navbar-dark + light navbar-custom */
  .ih-module-submenu {
    background: linear-gradient(90deg, #2f6e6e 0%, #387c7c 42%, #3d8585 100%);
    border: none;
    border-radius: 0;
    box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.12), 0 2px 4px rgba(0, 0, 0, 0.08);
    padding-top: 0.25rem;
    padding-bottom: 0.25rem;
    margin-top: 0;
    margin-bottom: 0;
    min-height: auto;
  }
  .ih-module-submenu .navbar-nav {
    gap: 0.15rem;
  }
  .ih-module-submenu .nav-link {
    color: rgba(255, 255, 255, 0.95) !important;
    font-weight: 500;
    padding: 0.45rem 0.95rem !important;
    border-radius: 4px;
    text-decoration: none !important;
    border-bottom: 2px solid transparent;
    transition: background-color 0.15s ease, color 0.15s ease, border-color 0.15s ease;
  }
  .ih-module-submenu .nav-link:hover,
  .ih-module-submenu .nav-link:focus {
    color: #fff !important;
    background-color: rgba(255, 255, 255, 0.14);
    border-bottom-color: rgba(255, 255, 255, 0.35);
  }
  .ih-module-submenu .nav-link.active {
    color: #fff !important;
    font-weight: 600;
    background-color: rgba(0, 0, 0, 0.12);
    border-bottom-color: #fff;
  }
</style>

<script type="text/javascript" src="${pageContext.request.contextPath}/moduleResources/ihmodule/js/bootstrap.min.js"></script>

<c:set var="ihNavUri" value="${pageContext.request.requestURI}" />

<nav class="navbar navbar-expand-lg navbar-dark ih-module-submenu" aria-label="IH module navigation">
  <div class="container-fluid px-3">
    <div class="collapse navbar-collapse show" id="navbarMenu">
      <ul class="navbar-nav flex-row flex-wrap align-items-center mb-0">

        <li class="nav-item">
          <a class="nav-link${fn:contains(ihNavUri, 'duplicatePatient') ? ' active' : ''}"
             href="${pageContext.request.contextPath}/module/ihmodule/duplicatePatient.form">
            Duplicate Patient
          </a>
        </li>
        <li class="nav-item">
          <a class="nav-link${fn:contains(ihNavUri, 'patientImportExport') ? ' active' : ''}"
             href="${pageContext.request.contextPath}/module/ihmodule/patientImportExport.form">
            Patient Import / Export
          </a>
        </li>
        <%-- Wrap with openmrs hasPrivilege privilege="view tdh report" when enforcing Reports access --%>
        <li class="nav-item">
          <a class="nav-link${fn:contains(ihNavUri, 'report.form') ? ' active' : ''}"
             href="${pageContext.request.contextPath}/module/ihmodule/report.form">
            Reports
          </a>
        </li>

      </ul>
    </div>
  </div>
</nav>
<c:if test="${not empty pre or not empty report}">
<h2 class="ih-module-submenu-meta mb-0 mt-2 px-3 small text-muted">
  <c:if test="${not empty pre}"><p class="mb-0">${pre}</p></c:if>
  <c:if test="${not empty report}"><p class="mb-0">${report}</p></c:if>
</h2>
</c:if>

