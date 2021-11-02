import pymc3 as pm              # Our main MCMC package
import arviz as az              # For describing output of pymc3
import matplotlib.pyplot as plt # Basic visualizations
import seaborn as sns           # Extended statistical visualizations
import numpy as np              # For using arrays

## Model
## There is a single underlying rate
## between 0 and 1 that is the probability of being left handed

## Parameters
## left_chance - the underlying rate of being left handed

## Data
## Given 26 people, we've had 2 lefties
n_people = 26
true_lefties = 2

## Creating the model
with pm.Model() as model:
    # Using the 'with pm.Model() as <ourname>' construction, we can add new random
    # variables to the model that can have other random variables as parameters.

    # Our prior beliefs about our parameters
    # We use the Beta(1,1) distribution instead of Uniform(0,1) because
    # computations are easier with some properties of Beta, but the distributions
    # are identical.
    left_chance = pm.Beta("left_chance", alpha=1, beta=1)
    
    # The binomial distribution gives the probability of the number of "successes" 
    # when running 'n' Bernoulli *processes*. So this ensures our data (true lefties)
    # is replicated in a 26 person simulated survey.
    # This is they way we specify the likelihood function, i.e., our MODEL, by
    # providing data in the "observed=" parameter. There can only be one of these.
    # In this example, our model is the binomial distribution, as that gives us
    # the probability of the data given the parameters (n_people and left_chance).
    lefties = pm.Binomial("lefties", n=n_people, p=left_chance, observed=true_lefties)

    # The updating step
    # Here we call the simulator to generate 10,000 samples from the posterior.
    # We do this 4 times, once for each "chain".
    # We also return_inferencedata for later use, and to suppress some warnings
    trace = pm.sample(10000, chains=4, return_inferencedata=True, cores=1)

    # Generating random survey data from the prior. This is called the
    # "prior predictive" distribution, which allows us to see how
    # our model performs if we had not updated our prior with data.
    prior = pm.sampling.sample_prior_predictive(
        samples=10000, var_names=["left_chance"])
    
    # Generating random survey data from the updated (posterior) model.
    posterior = pm.sample_posterior_predictive(
        trace, var_names=["left_chance"])

## Print details of the model
## The console will give us some descriptive statistics
## About our posterior parameters, like mean and standard deviation
print(az.summary(trace))

## Plot the prior, posterior, and maximum
## likelihood estimate
## We are using kernel density estimates to smooth the plot and make sure the
## area under each curve is 1.
## cut=0 stops our plot at 0 and 1 so that we don't see values that
## cannot exist
sns.kdeplot(np.array(prior["left_chance"]), cut=0)
sns.kdeplot(np.array(posterior["left_chance"]), cut=0)
plt.vlines(2/26, ymin=0, ymax=7.5, colors=["Green"])
plt.title("Chance of Left-Handedness")
plt.legend(["Prior", "Posterior", "MLE"])
plt.show()
