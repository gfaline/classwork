import pymc3 as pm
import scipy.stats as st
import arviz as az
import matplotlib.pyplot as plt
import numpy as np

# Estimating student lateness parameters using Bayesian inference

lateness_data = np.array([1, 1, 1, 2, 3, 5, 10,
                          0, 0, 0, -1, -1, -2,
                          -2, -3, -3, -3, -3,
                          -4, -4, -4, -4, -4, -4,
                          -5, -5, -5, -5, -5, -5, -5,
                          -6, -6, -6, -7, 10])

n_samples = 5000  # this should be higher for higher accuracy
# but during debugging (or demos) we can save some time.

# uninformative prior for the mean
prior_mean_mu = 0
prior_mean_std = 150

# informative prior for the mean
# prior_mean_mu = -4
# prior_mean_std = 1


""" Creating the model """
with pm.Model() as model:
    # The prior for the mean of the model is also a normal.
    mean_lateness = pm.Normal("mean_lateness",
                              mu=prior_mean_mu,  # defined above for convenience
                              sigma=prior_mean_std)  # defined above for convenience

    # prior for stddev of normal distribution. NOT normal of course. 
    # beware of differences in parametrization. beta[mathematica] = 1 / beta[PyMC3]
    # this is a very uninformative prior, because I had no idea.

    std_lateness = pm.Gamma("std_lateness", alpha=2, beta=0.2)

    # likelihood: the model, defining probability of data given parameters.
    lateness = pm.Normal("lateness", mu=mean_lateness, sigma=std_lateness,
                         observed=lateness_data)  # <- this makes it the LH!

    # The updating step
    # Here we call the simulator to generate samples from the posterior.
    # Multiple chains give more accuracy and allows for testing whether simulation converges.
    # We also return_inferencedata for later use, and to suppress some warnings
    trace = pm.sample(n_samples, chains=2,
                      return_inferencedata=True,
                      cores=1)

    # Generating random data from the prior. This is called the
    # "prior predictive" distribution, which allows us to see how
    # our model qualitatively performs if do not update our model with data.
    prior_predictive = pm.sampling.sample_prior_predictive(
        samples=n_samples, var_names=["mean_lateness", "std_lateness", "lateness"])

    # Generating random data from the updated (posterior) model.
    posterior_predictive = pm.sampling.sample_posterior_predictive(trace,
                                                                   samples=n_samples,
                                                                   var_names=["mean_lateness", "std_lateness",
                                                                              "lateness"])

"""Use Arviz to print some information about the fitted model """
print(az.summary(trace))

# plotting parameter mean, prior and posterior
with model:
    # plotting mean
    az.plot_dist(prior_predictive["mean_lateness"], color="orange", label="Prior")
    az.plot_dist(trace.posterior.mean_lateness, color="blue", label="Posterior")
    plt.xlim([-20, 20])
    plt.title("Lateness Mean")

    plt.show()

    # plotting standard deviation
    az.plot_dist(prior_predictive["std_lateness"], color="orange", label="Prior")
    az.plot_dist(trace.posterior.std_lateness, color="blue", label="Posterior")
    plt.xlim([0, 25])
    plt.title("Lateness Standard Deviation")
    plt.show()

    # Plotting our model
    az.plot_dist(prior_predictive["lateness"], color="orange", label="Prior")
    az.plot_dist(posterior_predictive["lateness"], color="blue", label="Posterior")
    az.plot_dist(lateness_data, color="green", label="data")
    plt.title("Lateness")
    plt.xlim([-20, 20])
    plt.show()
