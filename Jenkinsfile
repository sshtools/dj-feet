pipeline {
 	agent none
 	tools {
		maven 'Maven 3.9.0' 
		jdk 'Graal JDK 21' 
	}
	
	environment {
	    /* Constants / Configuration */
	    BUILD_PROPERTIES_ID = "b60f3998-d8fd-434b-b3c8-ed52aa52bc2e"
	    BUILD_PROPERTIES_NAME = "jadaptive.build.properties"
	    MAVEN_CONFIG_ID = "14324b85-c597-44e8-a575-61f925dba528"
	}

	stages {
		stage ('DJ-Feet Installers') {
			parallel {
				/*
				 * Linux Installers and Packages
				 */
				stage ('Linux DJ-Feet Installers') {
					agent {
						label 'install4j && linux'
					}
					steps {
						configFileProvider([
					 			configFile(
					 				fileId: "${env.BUILD_PROPERTIES_ID}",  
					 				replaceTokens: true,
					 				targetLocation: "${env.BUILD_PROPERTIES_NAME}",
					 				variable: 'BUILD_PROPERTIES'
					 			)
					 		]) {
					 		withMaven(
					 			globalMavenSettingsConfig: "${env.MAVEN_CONFIG_ID}"
					 		) {
					 		  	sh 'mvn -U -Dbuild.mediaTypes=unixInstaller,unixArchive,linuxRPM,linuxDeb ' +
					 		  	   '-P cross-platform ' +
					 		  	   '-Dbuild.projectProperties=$BUILD_PROPERTIES ' +
					 		  	   '-DbuildInstaller=true clean package'
					 		  	
					 		  	/* Stash installers */
			        			stash includes: 'installer/target/media/*', name: 'linux-djfeet'
			        			
			        			/* Stash updates.xml */
			        			dir('installer/target/media') {
									stash includes: 'updates.xml', name: 'linux-djfeet-updates-xml'
			        			}
					 		}
        				}
					}
				}
				
				/*
				 * Windows installers
				 */
				stage ('Windows DJ-Feet Installers') {
					agent {
						label 'install4j && windows'
					}
					steps {
						configFileProvider([
					 			configFile(
					 				fileId: "${env.BUILD_PROPERTIES_ID}",  
					 				replaceTokens: true,
					 				targetLocation: "${env.BUILD_PROPERTIES_NAME}",
					 				variable: 'BUILD_PROPERTIES'
					 			)
					 		]) {
					 		withMaven(
					 			globalMavenSettingsConfig: "${env.MAVEN_CONFIG_ID}"
					 		) {
					 		  	bat 'mvn -U -Dinstall4j.verbose=true -Dbuild.mediaTypes=windows,windowsArchive ' +
					 		  	    '"-Dbuild.projectProperties=%BUILD_PROPERTIES%" ' +
					 		  	   '-P cross-platform ' +
				 		  	        'clean package'
					 		  	
					 		  	/* Stash installers */
			        			stash includes: 'installer/target/media/*', name: 'windows-djfeet'
			        			
			        			/* Stash updates.xml */
			        			dir('installer/target/media') {
									stash includes: 'updates.xml', name: 'windows-djfeet-updates-xml'
			        			}
					 		}
        				}
					}
				}
				
				/*
				 * MacOS installers
				 */
				stage ('MacOS DJ-Feet Installers') {
					agent {
						label 'install4j && macos'
					}
					steps {
						configFileProvider([
					 			configFile(
					 				fileId: "${env.BUILD_PROPERTIES_ID}",  
					 				replaceTokens: true,
					 				targetLocation: "${env.BUILD_PROPERTIES_NAME}",
					 				variable: 'BUILD_PROPERTIES'
					 			)
					 		]) {
					 		withMaven(
					 			globalMavenSettingsConfig: "${env.MAVEN_CONFIG_ID}"
					 		) {
					 			// -Dinstall4j.disableNotarization=true 
					 		  	sh 'mvn -U -Dbuild.mediaTypes=macos,macosFolder,macosFolderArchive ' +
					 		  	   '-Dbuild.projectProperties=$BUILD_PROPERTIES ' +
					 		  	   '-P cross-platform ' +
					 		  	   'clean package'
					 		  	
					 		  	/* Stash installers */
			        			stash includes: 'installer/target/media/*', name: 'macos-djfeet'
			        			
			        			/* Stash updates.xml */
			        			dir('installer/target/media') {
									stash includes: 'updates.xml', name: 'macos-djfeet-updates-xml'
			        			}
					 		}
        				}
					}
				}
			}
		}
		
		stage ('Deploy') {
			agent {
				label 'linux'
			}
			steps {
    			
    			/* Clean */
    			withMaven(
		 			globalMavenSettingsConfig: "${env.MAVEN_CONFIG_ID}",
		 		) {
					sh 'mvn clean'
		 		}
			
				script {
					/* Create full version number from Maven POM version and the
					   build number */
					def pom = readMavenPom file: 'pom.xml'
					pom_version_array = pom.version.split('\\.')
					suffix_array = pom_version_array[2].split('-')
					env.FULL_VERSION = pom_version_array[0] + '.' + pom_version_array[1] + "." + suffix_array[0] + "-${BUILD_NUMBER}"
					echo 'Full Maven Version ' + env.FULL_VERSION
				}
				
				/* Unstash installers */
	 		  	unstash 'linux-djfeet'
	 		  	unstash 'windows-djfeet'
	 		  	unstash 'macos-djfeet'
	 		  	
				/* Unstash updates.xml */
	 		  	dir('installer/target/media-linux') {
	 		  		unstash 'linux-djfeet-updates-xml'
    			}
	 		  	dir('installer/target/media-windows') {
	 		  		unstash 'windows-djfeet-updates-xml'
    			}
	 		  	dir('installer/target/media-macos') {
	 		  		unstash 'macos-djfeet-updates-xml'
    			}
    			
    			/* Merge all updates.xml into one */
    			withMaven(
		 			globalMavenSettingsConfig: "${env.MAVEN_CONFIG_ID}",
		 		) {
					sh 'mvn -P merge-installers -pl installer com.sshtools:updatesxmlmerger-maven-plugin:merge'
		 		}
		 		
    			/* Upload all installers */
		 		s3Upload(
		 			consoleLogLevel: 'INFO', 
		 			dontSetBuildResultOnFailure: false, 
		 			dontWaitForConcurrentBuildCompletion: false, 
		 			entries: [[
		 				bucket: 'sshtools-public/dj-feet/' + env.FULL_VERSION, 
		 				noUploadOnFailure: true, 
		 				selectedRegion: 'eu-west-1', 
		 				sourceFile: 'installer/target/media/*', 
		 				storageClass: 'STANDARD', 
		 				useServerSideEncryption: false]], 
		 			pluginFailureResultConstraint: 'FAILURE', 
		 			profileName: 'JADAPTIVE Buckets', 
		 			userMetadata: []
		 		)
		 		s3Upload(
		 			consoleLogLevel: 'INFO', 
		 			dontSetBuildResultOnFailure: false, 
		 			dontWaitForConcurrentBuildCompletion: false, 
		 			entries: [[
		 				bucket: 'sshtools-public/dj-feet/' + env.FULL_VERSION, 
		 				noUploadOnFailure: true, 
		 				selectedRegion: 'eu-west-1', 
		 				sourceFile: 'installer/target/media/*', 
		 				storageClass: 'STANDARD', 
		 				useServerSideEncryption: false]], 
		 			pluginFailureResultConstraint: 'FAILURE', 
		 			profileName: 'JADAPTIVE Buckets', 
		 			userMetadata: []
		 		)
		 		
    			/* Copy the merged updates.xml to the nightly directory so updates can be seen
    			by anyone on this channel */
		 		s3Upload(
		 			consoleLogLevel: 'INFO', 
		 			dontSetBuildResultOnFailure: false, 
		 			dontWaitForConcurrentBuildCompletion: false, 
		 			entries: [[
		 				bucket: 'sshtools-public/dj-feet/continuous', 
		 				noUploadOnFailure: true, 
		 				selectedRegion: 'eu-west-1', 
		 				sourceFile: 'installer/target/media/updates.xml', 
		 				storageClass: 'STANDARD', 
		 				useServerSideEncryption: false]], 
		 			pluginFailureResultConstraint: 'FAILURE', 
		 			profileName: 'JADAPTIVE Buckets', 
		 			userMetadata: []
		 		)
			}					
		}		
	}
}